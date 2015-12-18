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
	 * Create a new search queue batch for the given action and add the batch to the search queue. This method can enhance the created batch by calling
	 * {@link #addRelatedEntries(SearchQueueBatch, SearchQueueEntryAction)} and thus adding entries for affected related object to the batch.
	 * 
	 * @param action
	 * @return
	 */
	SearchQueueBatch addIndexBatch(SearchQueueEntryAction action);

	/**
	 * Add related {@link SearchQueueEntry} to the batch which also need to be handled within the index for the given action. Normally this method should only
	 * be invoked when calling {@link #addIndexBatch(SearchQueueEntryAction)}.
	 * 
	 * @param batch
	 *            Batch to add new entries to
	 * @param action
	 */
	void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action);

}
