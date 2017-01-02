package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;

/**
 * @see SearchQueueBatch
 */
public class SearchQueueBatchImpl implements SearchQueueBatch {

	private String batchId;
	private long timestamp;
	private List<SearchQueueEntry> entries = new ArrayList<>();

	private static final Logger log = LoggerFactory.getLogger(SearchQueueBatchImpl.class);

	@Override
	public SearchQueueEntry addEntry(String uuid, String elementType, SearchQueueEntryAction action) {
		SearchQueueEntry entry = new SearchQueueEntryImpl();
		entry.setElementUuid(uuid);
		entry.setElementType(elementType);
		entry.setElementAction(action);
		entry.setTime(System.currentTimeMillis());
		return addEntry(entry);
	}

	@Override
	public SearchQueueEntry addEntry(SearchQueueEntry entry) {
		entries.add(entry);
		return entry;
	}

	@Override
	public List<? extends SearchQueueEntry> getEntries() {
		entries.sort((o1, o2) -> o1.getElementAction().compareTo(o2.getElementAction()));

		if (log.isDebugEnabled()) {
			for (SearchQueueEntry entry : entries) {
				log.debug("Loaded entry {" + entry.toString() + "} for batch {" + getBatchId() + "}");
			}
		}
		return entries;
	}

	@Override
	public String getBatchId() {
		return batchId;
	}

	@Override
	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
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

		// Process the batch
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

				//				// 4. Refresh index
				//				SearchProvider provider = MeshInternal.get().searchProvider();
				//				if (provider != null) {
				//					provider.refreshIndex();
				//				} else {
				//					log.error("Could not refresh index since the elasticsearch provider has not been initialized");
				//				}

			}).doOnError(error -> {
				// Add the batch back to the queue when an error occurs
				//TODO mark the batch as failed
				SearchQueue searchQueue = MeshInternal.get().searchQueue();
				log.error("Error while processing batch {" + this.getBatchId() + "}. Adding batch {" + this.getBatchId() + "} back to queue.", error);
				searchQueue.add(this);
			});
		});

	}

	@Override
	public void processSync(long timeout, TimeUnit unit) {
		if (!processAsync().await(timeout, unit)) {
			throw error(INTERNAL_SERVER_ERROR,
					"Batch {" + getBatchId() + "} did not finish in time. Timeout of {" + timeout + "} / {" + unit.name() + "} exceeded.");
		}
		MeshInternal.get().searchQueue().remove(this);
	}

	@Override
	public void processSync() {
		processSync(120, TimeUnit.SECONDS);
	}

	/**
	 * Delete the batch and all connected entries.
	 */
	public void delete() {
		entries.clear();
	}

}
