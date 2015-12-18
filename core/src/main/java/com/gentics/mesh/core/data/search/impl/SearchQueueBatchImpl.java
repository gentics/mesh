package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

public class SearchQueueBatchImpl extends MeshVertexImpl implements SearchQueueBatch {

	private static final Logger log = LoggerFactory.getLogger(SearchQueueBatchImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(SearchQueueBatchImpl.class);
	}

	@Override
	public void addEntry(String uuid, String type, SearchQueueEntryAction action, String indexType) {
		SearchQueueEntry entry = getGraph().addFramedVertex(SearchQueueEntryImpl.class);
		entry.setElementUuid(uuid);
		entry.setElementType(type);
		entry.setElementAction(action.getName());
		entry.setElementIndexType(indexType);
		addEntry(entry);
	}

	@Override
	public void addEntry(SearchQueueEntry batch) {
		setLinkOutTo(batch.getImpl(), HAS_ITEM);
	}

	@Override
	public void addEntry(MeshCoreVertex<?, ?> vertex, SearchQueueEntryAction action) {
		addEntry(vertex.getUuid(), vertex.getType(), action);
	}

	@Override
	public void addEntry(String uuid, String type, SearchQueueEntryAction action) {
		addEntry(uuid, type, action, null);
	}

	@Override
	public List<? extends SearchQueueEntry> getEntries() {
		return out(HAS_ITEM).has(SearchQueueEntryImpl.class).toListExplicit(SearchQueueEntryImpl.class);
	}

	@Override
	public String getBatchId() {
		return getProperty(BATCH_ID_PROPERTY_KEY);
	}

	@Override
	public void setBatchId(String batchId) {
		setProperty(BATCH_ID_PROPERTY_KEY, batchId);
	}

	@Override
	public long getTimestamp() {
		return getProperty("timestamp");
	}

	@Override
	public void setTimestamp(long timestamp) {
		setProperty("timestamp", timestamp);

	}

	@Override
	public void delete() {
		for (SearchQueueEntry entry : getEntries()) {
			entry.delete();
		}
		getVertex().remove();
	}

	@Override
	public void printDebug() {
		for (SearchQueueEntry entry : getEntries()) {
			log.debug("Entry {" + entry.toString() + "} in batch {" + getBatchId() + "}");
		}
	}

	@Override
	public void process(Handler<AsyncResult<Void>> handler) {

		if (log.isDebugEnabled()) {
			log.debug("Processing batch {" + getBatchId() + "}");
			printDebug();
		}
		Set<ObservableFuture<Void>> futures = new HashSet<>();
		for (SearchQueueEntry entry : getEntries()) {
			ObservableFuture<Void> obs = RxHelper.observableFuture();
			entry.process(obs.toHandler());
			futures.add(obs);
		}
		Observable.merge(futures).subscribe(item -> {
			if (log.isDebugEnabled()) {
				log.debug("Handled search queue item.");
			}
		} , error -> {
			log.error("Could not process batch {" + getBatchId() + "}.", error);
			handler.handle(Future.failedFuture(error));
		} , () -> {
			MeshSpringConfiguration springConfiguration = MeshSpringConfiguration.getInstance();
			Database db = springConfiguration.database();
			// We successfully finished this batch. Delete it.
			db.trx(tcDelete -> {
				reload();
				delete();
				tcDelete.complete();
			} , rhDelete -> {
				if (rhDelete.failed()) {
					handler.handle(Future.failedFuture(rhDelete.cause()));
				} else {
					SearchProvider provider = springConfiguration.searchProvider();
					if (provider != null) {
						provider.refreshIndex();
					} else {
						log.error("Could not refresh index since the elastic search provider has not been initalized");
					}
					handler.handle(Future.succeededFuture());
				}
			});

		});
	}

	@Override
	public void processBatch(ActionContext ac, Handler<AsyncResult<Future<Void>>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		// 1. Remove the batch from the queue
		db.trx(tc -> {
			SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
			searchQueue.reload();
			searchQueue.remove(this);
			tc.complete(searchQueue);
		} , sqrh -> {
			if (sqrh.failed()) {
				handler.handle(Future.failedFuture(sqrh.cause()));
			} else {
				// 2. Process the batch
				db.noTrx(txProcess -> {
					this.process(rh -> {
						// 3. Add the batch back to the queue when an error occurs
						if (rh.failed()) {
							db.trx(txAddBack -> {
								SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
								this.reload();
								log.error("Error while processing batch {" + this.getBatchId() + "}. Adding batch back to queue.", rh.cause());
								searchQueue.add(this);
								txAddBack.complete(this);
							} , txAddedBack -> {
								if (txAddedBack.failed()) {
									log.error("Failed to add batch {" + this.getBatchId() + "} batck to search queue.", txAddedBack.cause());
								}
							});
							// Inform the caller that processing failed
							handler.handle(failedFuture(BAD_REQUEST, "search_index_batch_process_failed", rh.cause()));
						} else {
							// Inform the caller that processing completed
							handler.handle(Future.succeededFuture());
						}
					});
				});
			}
		});
	}

}
