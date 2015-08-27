package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.util.VerticleHelper.fail;
import static io.vertx.core.http.HttpMethod.POST;
import static org.elasticsearch.client.Requests.refreshRequest;

import java.util.concurrent.atomic.AtomicInteger;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.AbstractIndexHandler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;

@Component
@Scope("singleton")
@SpringVerticle
public class SearchVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(SearchVerticle.class);

	@Autowired
	private org.elasticsearch.node.Node elasticSearchNode;

	@Autowired
	private Database db;

	@Autowired
	private SearchHandler searchHandler;

	public SearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addSearchEndpoints();
		addEventBusHandlers();
		// Trigger a search queue scan on startup in order to process old queue entries
		vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, true);
	}

	synchronized private void checkPendingQueueEntries(Handler<AsyncResult<Void>> handler) {
		try (Trx tx = db.trx()) {

			SearchQueue root = boot.meshRoot().getSearchQueue();
			AtomicInteger counter = new AtomicInteger();

			Handler<AsyncResult<JsonObject>> completeHandler = ach -> {
				if (counter.decrementAndGet() == 0) {
					elasticSearchNode.client().admin().indices().refresh(refreshRequest()).actionGet();
					handler.handle(Future.succeededFuture());
				}
			};

			while (true) {
				SearchQueueEntry entry = null;
				try {
					//TODO better to move this code into a mutex secured autoclosable
					SearchQueueEntry currentEntry;
					try (Trx txTake = db.trx()) {
						currentEntry = root.take();
						entry = currentEntry;
						txTake.success();
					}
					if (entry != null) {
						//TODO wait for all index events to complete
						counter.incrementAndGet();
						vertx.eventBus().send(AbstractIndexHandler.INDEX_EVENT_ADDRESS_PREFIX + entry.getElementType(), entry.getMessage(), rh -> {
							if (rh.failed()) {
								log.error("Indexing failed", rh.cause());
								//TODO handle this. Move item back into queue? queue is not a stack. broken entry would possibly directly retried.
							} else {
								log.info("Indexed element {" + currentEntry.getElementUuid() + ":" + currentEntry.getElementType() + "}");
							}
							completeHandler.handle(Future.succeededFuture(currentEntry.getMessage()));
						});
					} else {
						break;
					}
				} catch (InterruptedException e) {
					handler.handle(Future.failedFuture(e));
					// In case of an error put the entry back into the queue
					try (Trx txPutBack = db.trx()) {
						if (entry != null) {
							root.put(entry);
							txPutBack.success();
						}
					}
				}
			}
		}
	}

	private void addSearchEndpoints() {
		try (Trx tx = db.trx()) {
			addSearch("users", boot.userRoot(), UserListResponse.class);
			addSearch("groups", boot.groupRoot(), GroupListResponse.class);
			addSearch("role", boot.roleRoot(), RoleListResponse.class);
			addSearch("nodes", boot.nodeRoot(), NodeListResponse.class);
			addSearch("tags", boot.tagRoot(), TagListResponse.class);
			addSearch("tagFamilies", boot.tagFamilyRoot(), TagFamilyListResponse.class);
			addSearch("projects", boot.projectRoot(), ProjectListResponse.class);
			addSearch("schemas", boot.schemaContainerRoot(), SchemaListResponse.class);
			addSearch("microschemas", boot.microschemaContainerRoot(), MicroschemaListResponse.class);
		}
	}

	private <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void addSearch(String typeName,
			RootVertex<T> root, Class<RL> classOfRL) {
		Route postRoute = route("/" + typeName).method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			try {
				searchHandler.handleSearch(rc, root, classOfRL);
			} catch (Exception e) {
				fail(rc, "search_error_query");
			}
		});
	}

	private void addEventBusHandlers() {
		EventBus bus = vertx.eventBus();

		// Message bus consumer that handles events that indicate changes to the search queue 
		bus.consumer(SEARCH_QUEUE_ENTRY_ADDRESS, mh -> {
			checkPendingQueueEntries(rh -> {
				if (rh.failed()) {
					mh.fail(500, rh.cause().getMessage());
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Handled all pending search queue entries.");
					}
					mh.reply(true);
				}
			});
		});

	}

}
