package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

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
	public static void checkIndices(Database database) {
		database.addVertexType(SearchQueueBatchImpl.class);
	}

	@Override
	public void addEntry(String uuid, String elementType, SearchQueueEntryAction action, String indexType, Collection<Tuple<String, Object>> customProperties) {
		SearchQueueEntry entry = getGraph().addFramedVertex(SearchQueueEntryImpl.class);
		entry.setElementUuid(uuid);
		entry.setElementType(elementType);
		entry.setElementAction(action.getName());
		entry.setElementIndexType(indexType);

		if (customProperties != null) {
			for (Tuple<String, Object> custom : customProperties) {
				entry.setCustomProperty(custom.v1(), custom.v2());
			}
		}

		addEntry(entry);
	}

	@Override
	public void addEntry(SearchQueueEntry batch) {
		setUniqueLinkOutTo(batch.getImpl(), HAS_ITEM);
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
	public Observable<SearchQueueBatch> process() {

		MeshSpringConfiguration springConfiguration = MeshSpringConfiguration.getInstance();
		Database db = springConfiguration.database();

		return db.noTrx(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Processing batch {" + getBatchId() + "}");
				printDebug();
			}
			List<Observable<Void>> obs = new ArrayList<>();
			for (SearchQueueEntry entry : getEntries()) {
				obs.add(entry.process());
			}

			obs.add(Observable.just(null));
			Observable<SearchQueueBatch> mergedObs = Observable.merge(obs).last().map(done -> this);
			mergedObs = mergedObs.doOnCompleted(() -> {
				if (log.isDebugEnabled()) {
					log.debug("Handled all search queue items.");
				}

				// We successfully finished this batch. Delete it.
				db.trx(() -> {
					reload();
					delete(null);
					return null;
				});
				// Refresh index
				SearchProvider provider = springConfiguration.searchProvider();
				if (provider != null) {
					provider.refreshIndex();
				} else {
					log.error("Could not refresh index since the elasticsearch provider has not been initalized");
				}
			});

			//TODO define what to do when an error during processing occurs. Should we fail somehow? Should we mark the failed batch? Retry the processing?
			// mergedObs.doOnError(error -> {
			// return null;
			// });
			return mergedObs;
		});

	}

	@Override
	public Observable<SearchQueueBatch> process(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		// 1. Remove the batch from the queue
		SearchQueueBatch removedBatch = db.trx(() -> {
			SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
			searchQueue.reload();
			searchQueue.remove(this);
			return this;
		});

		// 2. Process the batch
		return db.noTrx(() -> {
			return removedBatch.process().doOnError(error -> {
				// Add the batch back to the queue when an error occurs
				db.trx(() -> {
					SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
					this.reload();
					log.error("Error while processing batch {" + this.getBatchId() + "}. Adding batch {" + this.getBatchId() + "} back to queue.",
							error);
					searchQueue.add(this);
					return this;
				});
				// .doOnError(() -> {
				// log.error("Failed to add batch {" + this.getBatchId() + "} back to search queue.", txAddedBack.cause());
				// // Inform the caller that processing failed
				// throw error(BAD_REQUEST, "search_index_batch_process_failed", error);
				// });
			});

		});

	}

}
