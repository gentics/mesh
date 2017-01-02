package com.gentics.mesh.core.data.search;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.core.data.MeshCoreVertex;

import rx.Completable;

/**
 * A batch of search queue entries. Usually a batch groups those elements that need to be updated in order to sync the search index with the graph database
 * changes.
 */
public interface SearchQueueBatch {

	/**
	 * Add an entry to this batch.
	 * 
	 * @param uuid
	 * @param type
	 * @param action
	 * @return Created entry
	 */
	default SearchQueueEntry addEntry(String uuid, String type, SearchQueueEntryAction action) {
		return addEntry(uuid, type, action, null);
	}

	/**
	 * Add an entry to this batch.
	 * 
	 * @param vertex
	 * @param action
	 * @return Created entry
	 */
	default SearchQueueEntry addEntry(MeshCoreVertex<?, ?> vertex, SearchQueueEntryAction action) {
		return addEntry(vertex.getUuid(), vertex.getType(), action);
	}

	/**
	 * Add an entry to this batch.
	 * 
	 * @param uuid
	 *            Uuid of the element to be added
	 * @param elementType
	 *            Type of the element to be added
	 * @param action
	 * @param indexType
	 *            Search index type
	 * @return Created entry
	 */
	default SearchQueueEntry addEntry(String uuid, String elementType, SearchQueueEntryAction action, String indexType) {
		return addEntry(uuid, elementType, action, indexType);
	}

	/**
	 * Add an entry to this batch.
	 * 
	 * @param entry
	 * @return Added entry
	 */
	SearchQueueEntry addEntry(SearchQueueEntry entry);

	/**
	 * Return a list of entries for this batch.
	 * 
	 * @return
	 */
	List<? extends SearchQueueEntry> getEntries();

	/**
	 * Find the entry with the given uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	default Optional<? extends SearchQueueEntry> findEntryByUuid(String uuid) {
		return getEntries().stream().filter(e -> e.getElementUuid().equals(uuid)).findAny();
	}

	/**
	 * Set the batch id.
	 * 
	 * @param batchId
	 */
	void setBatchId(String batchId);

	/**
	 * Return the batch id for this batch.
	 * 
	 * @return
	 */
	String getBatchId();

	/**
	 * Process this batch by invoking process on all batch entries.
	 */
	Completable processAsync();

	/**
	 * Process this batch blocking and fail if the given timeout was exceeded.
	 * 
	 * @param timeout
	 * @param unit
	 */
	void processSync(long timeout, TimeUnit unit);

	/**
	 * Process this batch and block until it finishes. Apply a default timeout on this operation.
	 */
	void processSync();

	/**
	 * Print debug output.
	 */
	void printDebug();

	/**
	 * Set the creation timestamp of the search queue batch.
	 * 
	 * @param currentTimeMillis
	 */
	void setTimestamp(long currentTimeMillis);

	/**
	 * Return the timestamp when the batch was created.
	 * 
	 * @return
	 */
	long getTimestamp();

}
