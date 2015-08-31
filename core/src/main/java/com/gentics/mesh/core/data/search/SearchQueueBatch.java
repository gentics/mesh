package com.gentics.mesh.core.data.search;

import java.util.List;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshVertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

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
	void addEntry(String uuid, String type, SearchQueueEntryAction action);

	/**
	 * Add an entry to this batch.
	 * 
	 * @param vertex
	 * @param action
	 */
	void addEntry(GenericVertex<?> vertex, SearchQueueEntryAction action);

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
	 * 
	 * @param handler
	 */
	void process(Handler<AsyncResult<Void>> handler);

	void printDebug();
}
