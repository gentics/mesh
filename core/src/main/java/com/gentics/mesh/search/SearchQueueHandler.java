package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static org.elasticsearch.client.Requests.refreshRequest;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.search.index.AbstractIndexHandler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

@Component
public class SearchQueueHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(SearchQueueHandler.class);

	@Autowired
	private ElasticSearchProvider elasticSearchProvider;

	@PostConstruct
	public void init() {
		addEventBusHandlers();
	}

	public void triggerQueueProcessing() {
		// Trigger a search queue scan on startup in order to process old queue entries
		Mesh.vertx().eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, true);
		addEventBusHandlers();
	}

	private void addEventBusHandlers() {
		EventBus bus = Mesh.vertx().eventBus();

		// Message bus consumer that handles events that indicate changes to the search queue 
		bus.consumer(SEARCH_QUEUE_ENTRY_ADDRESS, (Message<String> mh) -> {
			String batchId = mh.body();

			// No batchId specified. Handle all remaining batches.
			if (StringUtils.isEmpty(batchId)) {
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
			} else {
				handleBatch(batchId, rh -> {
					if (rh.failed()) {
						mh.fail(500, rh.cause().getMessage());
					} else {
						if (log.isDebugEnabled()) {
							log.debug("Handled batch {" + batchId + "}.");
						}
						mh.reply(true);
					}
				});
			}
		});

	}

	private void handleBatch(String batchId, Handler<AsyncResult<Void>> handler) {
		try (Trx tx = db.trx()) {
			SearchQueue queue = boot.meshRoot().getSearchQueue();
			SearchQueueBatch batch;
			try (Trx txTake = db.trx()) {
				batch = queue.take(batchId);
				txTake.success();
				txTake.commit();
			}
			if (batch != null) {
				Set<ObservableFuture<ActionResponse>> futures = new HashSet<>();
				for (SearchQueueEntry entry : batch.getEntries()) {
					ObservableFuture<Message<Void>> obs = RxHelper.observableFuture();
					vertx.eventBus().send(AbstractIndexHandler.INDEX_EVENT_ADDRESS_PREFIX + entry.getElementType(), entry.getMessage(),
							obs.toHandler());
				}
				Observable.merge(futures).subscribe(item -> {
					if (log.isDebugEnabled()) {
						log.debug("Handled entry");
					}
				} , error -> {
					//TODO put batch back into queue and mark it as broken?
					log.error("Error while processing batch {" + batchId + "}", error);
					handler.handle(Future.failedFuture(error));
				} , () -> {
					if (log.isDebugEnabled()) {
						log.debug("Handled all entries in batch {" + batchId + "}");
					}
					try (Trx txDelete = db.trx()) {
						batch.reload();
						batch.delete();
						txDelete.success();
						txDelete.commit();
					}
					MeshSpringConfiguration.getMeshSpringConfiguration().elasticSearchProvider().refreshIndex();
					handler.handle(Future.succeededFuture());
				});
			} else {
				handler.handle(Future.failedFuture("Batch {" + batchId + "} could not be found."));
			}
		}

	}

	synchronized private void checkPendingQueueEntries(Handler<AsyncResult<Void>> handler) {
		try (Trx tx = db.trx()) {

			SearchQueue root = boot.meshRoot().getSearchQueue();
			AtomicInteger counter = new AtomicInteger();

			Handler<AsyncResult<JsonObject>> completeHandler = ach -> {
				if (counter.decrementAndGet() == 0) {
					elasticSearchProvider.getNode().client().admin().indices().refresh(refreshRequest()).actionGet();
					handler.handle(Future.succeededFuture());
				}
			};

			while (true) {
				SearchQueueBatch batch = null;
				try {
					//TODO better to move this code into a mutex secured autoclosable
					SearchQueueBatch currentBatch;
					try (Trx txTake = db.trx()) {
						currentBatch = root.take();
						batch = currentBatch;
						txTake.success();
					}
					if (batch != null) {
						for (SearchQueueEntry entry : batch.getEntries()) {
							//TODO wait for all index events to complete
							counter.incrementAndGet();
							vertx.eventBus().send(AbstractIndexHandler.INDEX_EVENT_ADDRESS_PREFIX + entry.getElementType(), entry.getMessage(),
									rh -> {
										if (rh.failed()) {
											log.error("Indexing failed", rh.cause());
											//TODO handle this. Move item back into queue? queue is not a stack. broken entry would possibly directly retried.
										} else {
											log.info("Indexed element {" + entry.getElementUuid() + ":" + entry.getElementType() + "}");
										}
										completeHandler.handle(Future.succeededFuture(entry.getMessage()));
									});
						}
					} else {
						break;
					}
				} catch (InterruptedException e) {
					handler.handle(Future.failedFuture(e));
					// In case of an error put the entry back into the queue
					try (Trx txPutBack = db.trx()) {
						if (batch != null) {
							root.addBatch(batch);
							txPutBack.success();
						}
					}
				}
			}
		}
	}

}
