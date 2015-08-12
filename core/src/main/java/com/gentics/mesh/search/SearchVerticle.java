package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.VerticleHelper.getPagingInfo;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.responde;
import static io.vertx.core.http.HttpMethod.POST;
import static org.elasticsearch.client.Requests.refreshRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
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
import com.gentics.mesh.search.index.AbstractIndexHandler;
import com.gentics.mesh.search.index.GroupIndexHandler;
import com.gentics.mesh.search.index.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.search.index.ProjectIndexHandler;
import com.gentics.mesh.search.index.RoleIndexHandler;
import com.gentics.mesh.search.index.SchemaContainerIndexHandler;
import com.gentics.mesh.search.index.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.TagIndexHandler;
import com.gentics.mesh.search.index.UserIndexHandler;
import com.gentics.mesh.util.BlueprintTransaction;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

@Component
@Scope("singleton")
@SpringVerticle
public class SearchVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(SearchVerticle.class);

	@Autowired
	private org.elasticsearch.node.Node elasticSearchNode;

	@Autowired
	private UserIndexHandler userIndexHandler;

	@Autowired
	private GroupIndexHandler groupIndexHandler;

	@Autowired
	private RoleIndexHandler roleIndexHandler;

	@Autowired
	private ProjectIndexHandler projectIndexHandler;

	@Autowired
	private TagIndexHandler tagIndexHandler;

	@Autowired
	private NodeIndexHandler nodeIndexHandler;

	@Autowired
	private TagFamilyIndexHandler tagFamilyIndexHandler;

	@Autowired
	private SchemaContainerIndexHandler schemaContainerIndexHandler;

	@Autowired
	private MicroschemaContainerIndexHandler microschemaContainerIndexHandler;

	public SearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addSearchEndpoints();
		addEventBusHandlers();
		vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, true);
	}

	synchronized private void checkPendingQueueEntries(Handler<AsyncResult<Void>> handler) {
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
				SearchQueueEntry currentEntry = root.take();
				entry = currentEntry;
				if (entry != null) {
					//TODO wait for all index events to complete
					counter.incrementAndGet();
					vertx.eventBus().send(AbstractIndexHandler.INDEX_EVENT_ADDRESS_PREFIX + entry.getElementType(), entry.getMessage(), rh -> {
						if (rh.failed()) {
							//TODO handle this. Move item back into queue? queue is not a stack. broken entry would possibly directly retried.
						} else {
							log.info("Indexed element {" + currentEntry.getUuid() + "}");
						}
						completeHandler.handle(Future.succeededFuture(currentEntry.getMessage()));
					});
				} else {
					break;
				}
			} catch (InterruptedException e) {
				handler.handle(Future.failedFuture(e));
				// In case of an error put the entry back into the queue
				if (entry != null) {
					root.put(entry);
				}
			}
		}
	}

	private void addSearchEndpoints() {
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
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
				handleSearch(rc, root, classOfRL);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	private <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void handleSearch(RoutingContext rc,
			RootVertex<T> rootVertex, Class<RL> classOfRL) throws InstantiationException, IllegalAccessException, InvalidArgumentException {

		PagingInfo pagingInfo = getPagingInfo(rc);
		if (pagingInfo.getPage() < 1) {
			throw new InvalidArgumentException("The page must always be positive");
		}
		if (pagingInfo.getPerPage() < 1) {
			throw new InvalidArgumentException("The pageSize must always be positive");
		}

		RL listResponse = classOfRL.newInstance();
		MeshAuthUser requestUser = getUser(rc);
		Client client = elasticSearchNode.client();

		SearchRequestBuilder builder = client.prepareSearch().setQuery(rc.getBodyAsString());
		builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

		/*
		 * TODO, FIXME This a very crude hack but we need to handle paging ourself for now. In order to avoid such nasty ways of paging a custom ES plugin has
		 * to be written that deals with Document Level Permissions/Security (common known as DLS)
		 */
		builder.setSize(Integer.MAX_VALUE);
		builder.setFrom(0);

		SearchResponse response = builder.execute().actionGet();

		List<T> elements = new ArrayList<>();
		for (SearchHit hit : response.getHits()) {
			String uuid = hit.getId();

			// Locate the node
			rootVertex.findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					T element = rh.result();
					/* Check permissions */
					if (requestUser.hasPermission(element, GraphPermission.READ_PERM)) {
						elements.add(element);
					}
				} else {
					// TODO log error info?
				}
			});
		}

		// Internally we start with page 0
		int page = pagingInfo.getPage() - 1;

		int low = page * pagingInfo.getPerPage();
		int upper = low + pagingInfo.getPerPage() - 1;

		int n = 0;
		for (T element : elements) {
			//Only transform elements that we want to list in our resultset
			if (n >= low && n <= upper) {
				// Transform node and add it to the list of nodes
				element.transformToRest(rc, th -> {
					listResponse.getData().add(th.result());
				});
			}
			n++;
		}

		PagingMetaInfo metainfo = new PagingMetaInfo();
		int totalPages = (int) Math.ceil(elements.size() / (double) pagingInfo.getPerPage());
		// Cap totalpages to 1
		if (totalPages == 0) {
			totalPages = 1;
		}
		metainfo.setTotalCount(elements.size());

		metainfo.setCurrentPage(pagingInfo.getPage());
		metainfo.setPageCount(totalPages);
		metainfo.setPerPage(pagingInfo.getPerPage());
		listResponse.setMetainfo(metainfo);

		responde(rc, toJson(listResponse));

	}

	private void addEventBusHandlers() {
		EventBus bus = vertx.eventBus();

		bus.consumer(SEARCH_QUEUE_ENTRY_ADDRESS, mh -> {
			checkPendingQueueEntries(rh -> {
				if (rh.failed()) {
					mh.fail(500, rh.cause().getMessage());
				} else {
					mh.reply(true);
				}
			});
		});

	}

}
