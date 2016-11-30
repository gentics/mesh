package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;

/**
 * @see SearchQueueBatch
 */
public class SearchQueueBatchImpl extends MeshVertexImpl implements SearchQueueBatch {

	private static final Logger log = LoggerFactory.getLogger(SearchQueueBatchImpl.class);

	/**
	 * Setup vertex types and indices for search queue vertices.
	 * 
	 * @param database
	 */
	public static void init(Database database) {
		database.addVertexType(SearchQueueBatchImpl.class, MeshVertexImpl.class);
	}

	@Override
	public SearchQueueEntry addEntry(String uuid, String elementType, SearchQueueEntryAction action) {
		SearchQueueEntry entry = getGraph().addFramedVertex(SearchQueueEntryImpl.class);
		entry.setElementUuid(uuid);
		entry.setElementType(elementType);
		entry.setElementAction(action.getName());
		entry.setTime(System.currentTimeMillis());
		return addEntry(entry);
	}

	@Override
	public SearchQueueEntry addEntry(SearchQueueEntry entry) {
		linkOut(entry.getImpl(), HAS_ITEM);
		return entry;
	}

	@Override
	public List<? extends SearchQueueEntry> getEntries() {
		List<? extends SearchQueueEntryImpl> list = out(HAS_ITEM).order((o1, o2) -> {
			String actionA = o1.getProperty(SearchQueueEntryImpl.ACTION_KEY);
			String actionB = o1.getProperty(SearchQueueEntryImpl.ACTION_KEY);
			return SearchQueueEntryAction.valueOfName(actionA).compareTo(SearchQueueEntryAction.valueOfName(actionB));
		}).toListExplicit(SearchQueueEntryImpl.class);

		if (log.isDebugEnabled()) {
			for (SearchQueueEntry entry : list) {
				log.debug("Loaded entry {" + entry.toString() + "} for batch {" + getBatchId() + "}");
			}
		}
		return list;
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
	public void delete(SearchQueueBatch batch) {
		for (SearchQueueEntry entry : getEntries()) {
			entry.delete(batch);
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
	public Completable processAsync() {
		Database db = MeshInternal.get().database();
		BootstrapInitializer boot = MeshInternal.get().boot();

		// 1. Remove the batch from the queue
		db.tx(() -> {
			SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
			searchQueue.reload();
			searchQueue.remove(this);
			return this;
		});

		// 2. Process the batch
		return db.noTx(() -> {

			Completable obs = Completable.complete();
			try (NoTx noTrx = db.noTx()) {
				List<Completable> entryList = getEntries().stream().map(entry -> entry.process()).collect(Collectors.toList());
				if (!entryList.isEmpty()) {
					obs = Completable.concat(entryList);
				}
			}

			return obs.doOnCompleted(() -> {
				if (log.isDebugEnabled()) {
					log.debug("Handled all search queue items.");
				}

				// 3. We successfully finished this batch. Delete it.
				db.tx(() -> {
					reload();
					delete(null);
					return null;
				});

//				// 4. Refresh index
//				SearchProvider provider = MeshInternal.get().searchProvider();
//				if (provider != null) {
//					provider.refreshIndex();
//				} else {
//					log.error("Could not refresh index since the elasticsearch provider has not been initalized");
//				}

			}).doOnError(error -> {
				// Add the batch back to the queue when an error occurs
				db.tx(() -> {
					//TODO mark the batch as failed
					SearchQueue searchQueue = boot.meshRoot().getSearchQueue();

					this.reload();
					log.error("Error while processing batch {" + this.getBatchId() + "}. Adding batch {" + this.getBatchId() + "} back to queue.",
							error);
					searchQueue.add(this);
					return this;
				});
			});
		});

	}

	@Override
	public void processSync(long timeout, TimeUnit unit) {
		if (!processAsync().await(timeout, unit)) {
			throw error(INTERNAL_SERVER_ERROR,
					"Batch {" + getBatchId() + "} did not finish in time. Timeout of {" + timeout + "} / {" + unit.name() + "} exceeded.");
		}
	}

	@Override
	public void processSync() {
		processSync(120, TimeUnit.SECONDS);
	}

	/**
	 * Delete the batch and all connected entries.
	 */
	public void delete() {
		for (SearchQueueEntry entry : getEntries()) {
			entry.delete();
		}
		getElement().remove();
	}

}
