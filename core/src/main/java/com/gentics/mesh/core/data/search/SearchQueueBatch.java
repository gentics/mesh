package com.gentics.mesh.core.data.search;

import java.util.List;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
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
	void addEntry(MeshCoreVertex<?, ?> vertex, SearchQueueEntryAction action);

	/**
	 * Add an entry to this batch.
	 * 
	 * @param uuid
	 * @param type
	 * @param action
	 * @param indexType
	 */
	void addEntry(String uuid, String type, SearchQueueEntryAction action, String indexType);

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
	 * @param batch
	 *            Batch to be processed
	 * @param handler
	 *            Result handler that will be invoked on completion or error
	 */
	void process(InternalActionContext ac, Handler<AsyncResult<Void>> handler);

	 <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void processOrFail(InternalActionContext ac, Handler<AsyncResult<T>> handler, T element);


}
