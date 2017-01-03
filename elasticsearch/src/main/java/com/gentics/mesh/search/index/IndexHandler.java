package com.gentics.mesh.search.index;

import java.util.Map;
import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueEntry;

import rx.Completable;

/**
 * Index handlers are used to interact with the search provider index on a type specific level. Each domain model in mesh which is searchable needs to implement
 * an index handler in order to interact with search index specific documents in the index (CRUD on search index documents).
 */
public interface IndexHandler {

	/**
	 * Index handler key for the registry.
	 * 
	 * @return handler key
	 */
	String getKey();

	/**
	 * Clear the index. This will effectively remove all documents from the index without removing the index itself.
	 * 
	 * @return
	 */
	Completable clearIndex();

	/**
	 * Initialise the search index by creating it first and setting the mapping afterwards.
	 * 
	 * @return
	 */
	Completable init();

	/**
	 * Handle the search queue update mapping entry.
	 * 
	 * @param entry
	 * @return
	 */
	Completable updateMapping(SearchQueueEntry entry);

	/**
	 * Handle search index action.
	 * 
	 * @param entry
	 *            search queue entry
	 * @return
	 */
	Completable handleAction(SearchQueueEntry entry);

	/**
	 * Delete the document with the given UUID and document type from the search index.
	 * 
	 * @param entry
	 *            search queue entry
	 * @return
	 */
	Completable delete(SearchQueueEntry entry);

	/**
	 * Load the given element and invoke store(T element) to store it in the index.
	 * 
	 * @param entry
	 *            search queue entry
	 * @return
	 */
	Completable store(SearchQueueEntry entry);

	/**
	 * Reindex all documents for the type which the handler is capable of.
	 * 
	 * @return
	 */
	Completable reindexAll();

	/**
	 * Load a map which contains sets of document types per index. The key of the map is the index name.
	 * 
	 * @return Index info
	 */
	Map<String, Set<String>> getIndices();

	/**
	 * Get the names of all selected indices. The action context will be examined to determine the project scope and the release scope. If possible even the
	 * version type will be extracted from the action context in order to generate the set of indices which are selected.
	 * 
	 * @param ac
	 *            action context
	 * @return name of selected indices
	 */
	Set<String> getSelectedIndices(InternalActionContext ac);

	/**
	 * Get the permission required to read the elements found in the index.
	 * 
	 * @param ac
	 *            action context
	 * @return read permission
	 */
	default GraphPermission getReadPermission(InternalActionContext ac) {
		return GraphPermission.READ_PERM;
	}
}
