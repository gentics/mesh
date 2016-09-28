package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.graphdb.model.MeshElement;

/**
 * An {@link IndexableElement} is an a vertex which can be added to the search index.
 *
 */
public interface IndexableElement extends MeshElement {

	/**
	 * Return the type of the vertex.
	 * 
	 * @return Vertex type
	 */
	String getType();

	/**
	 * Add an index batch entry to the given batch.
	 * 
	 * @param batch
	 * @param action
	 * @return
	 */
	SearchQueueBatch addIndexBatchEntry(SearchQueueBatch batch, SearchQueueEntryAction action);

	/**
	 * Add related {@link SearchQueueEntry} to the batch which also need to be handled within the index for the given action. Normally this method should only
	 * be invoked when calling {@link #addIndexBatchEntry(SearchQueueBatch, SearchQueueEntryAction)}.
	 * 
	 * @param batch
	 *            Batch to add new entries to
	 * @param action
	 */
	void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action);

}
