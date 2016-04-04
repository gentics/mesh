package com.gentics.mesh.core.data.search;

import java.util.Collection;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.util.Tuple;
import rx.Observable;

/**
 * A batch of search queue entries. Usually a batch groups those elements that need to be updated in order to sync the search index with the graph database
 * changes.
 */
public interface SearchQueueBatch extends MeshVertex {

	public static final String BATCH_ID_PROPERTY_KEY = "batch_id";

	/**
	 * Add an entry to this batch.
	 * 
	 * @param uuid
	 * @param type
	 * @param action
	 */
	default void addEntry(String uuid, String type, SearchQueueEntryAction action) {
		addEntry(uuid, type, action, null, null);
	}

	/**
	 * Add an entry to this batch.
	 * 
	 * @param vertex
	 * @param action
	 */
	default void addEntry(MeshCoreVertex<?, ?> vertex, SearchQueueEntryAction action) {
		addEntry(vertex.getUuid(), vertex.getType(), action, null, null);
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
	 */
	default void addEntry(String uuid, String elementType, SearchQueueEntryAction action, String indexType) {
		addEntry(uuid, elementType, action, indexType, null);
	}

	/**
	 * Add an entry to this batch.
	 * 
	 * @param uuid Uuid of the element to be added
	 * @param elementType Type of the element to be added
	 * @param action
	 * @param indexType
	 * @param customProperties
	 */
	void addEntry(String uuid, String elementType, SearchQueueEntryAction action, String indexType,
			Collection<Tuple<String, Object>> customProperties);

	/**
	 * Add an entry to this batch.
	 * 
	 * @param entry
	 */
	void addEntry(SearchQueueEntry entry);

	/**
	 * Return a list of entries for this batch.
	 * 
	 * @return
	 */
	List<? extends SearchQueueEntry> getEntries();

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
	Observable<SearchQueueBatch> process();

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

	/**
	 * Process the given batch and call the handler when the batch was processed.
	 * 
	 * @param ac
	 */
	Observable<SearchQueueBatch> process(InternalActionContext ac);

}
