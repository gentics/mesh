package com.gentics.mesh.search.index;

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
	 * Index handler key for the registry
	 * 
	 * @return handler key
	 */
	String getKey();

	/**
	 * Clear the index.
	 * 
	 * @return
	 */
	Completable clearIndex();

	/**
	 * Initialize the search index by creating it first and setting the mapping afterwards.
	 * 
	 * @return
	 */
	Completable init();

	/**
	 * Create the search index.
	 * 
	 * @return
	 */
	Completable createIndex();

	/**
	 * Handle the search queue upate mapping entry.
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
	 * @param uuid
	 * @param documentType
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
	 * Get the name of all indices.
	 * 
	 * @return name of all indices
	 */
	Set<String> getIndices();

	/**
	 * Get the name of all affected indices.
	 * 
	 * @param ac
	 *            action context
	 * @return name of affected indices
	 */
	Set<String> getAffectedIndices(InternalActionContext ac);

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
