package com.gentics.mesh.search.index;

import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueEntry;

import rx.Observable;

/**
 * Index handlers are used to interact with the search provider index on a type specific level. Each domain model in mesh which is searchable needs to implement
 * an index handler in order to interact with search index specific documents in the index (CRUD on search index documents).
 */
public interface IndexHandler {
	/**
	 * Index handler key for the registry
	 * @return handler key
	 */
	String getKey();

	/**
	 * Clear the index.
	 * 
	 * @return
	 */
	Observable<Void> clearIndex();

	/**
	 * Initialize the search index by creating it first and setting the mapping afterwards.
	 * 
	 * @return
	 */
	Observable<Void> init();

	/**
	 * Create the search index.
	 * 
	 * @return
	 */
	Observable<Void> createIndex();

	/**
	 * Update the index specific mapping.
	 * 
	 * @return
	 */
	Observable<Void> updateMapping();

	/**
	 * Update the mapping for the given index
	 * 
	 * @param indexName
	 *            index name
	 * @return
	 */
	Observable<Void> updateMapping(String indexName);

	/**
	 * Handle search index action
	 * @param entry search queue entry
	 * @return
	 */
	Observable<Void> handleAction(SearchQueueEntry entry);

	/**
	 * Delete the document with the given uuid and document type from the search index.
	 * 
	 * @param uuid
	 * @param documentType
	 * @param entry search queue entry
	 * @return
	 */
	Observable<Void> delete(String uuid, String documentType, SearchQueueEntry entry);

	/**
	 * Load the given element and invoke store(T element) to store it in the index.
	 * 
	 * @param uuid
	 * @param documentType
	 * @param entry search queue entry
	 * @return
	 */
	Observable<Void> store(String uuid, String documentType, SearchQueueEntry entry);

	/**
	 * Reindex all documents for the type which the handler is capable of.
	 * 
	 * @return
	 */
	Observable<Void> reindexAll();

	/**
	 * Get the name of all indices
	 * @return name of all indices
	 */
	Set<String> getIndices();

	/**
	 * Get the name of all affected indices
	 * @param ac action context
	 * @return name of affected indices
	 */
	Set<String> getAffectedIndices(InternalActionContext ac);

	/**
	 * Get the permission required to read the elements found in the index
	 * @param ac action context
	 * @return read permission
	 */
	default GraphPermission getReadPermission(InternalActionContext ac) {
		return GraphPermission.READ_PERM;
	}
}
