package com.gentics.mesh.search.index;

import rx.Observable;

/**
 * Index handlers are used to interact with the search provider index on a type specific level. Each domain model in mesh which is searchable needs to implement
 * an index handler in order to interact with search index specific documents in the index (CRUD on search index documents).
 */
public interface IndexHandler {

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
	 * Handle a search index action. An action will modify the search index (delete, update, create)
	 * 
	 * @param uuid
	 *            Uuid of the document that should be handled
	 * @param actionName
	 *            Type of the action (delete, update, create)
	 * @param indexType
	 *            Type of the index
	 */
	Observable<Void> handleAction(String uuid, String actionName, String indexType);

	/**
	 * Delete the document with the given uuid and type from the search index.
	 * 
	 * @param uuid
	 * @param type
	 * @return
	 */
	Observable<Void> delete(String uuid, String type);

	/**
	 * Load the given element and invoke store(T element) to store it in the index.
	 * 
	 * @param uuid
	 * @param indexType
	 * @return
	 */
	Observable<Void> store(String uuid, String indexType);

	/**
	 * Update the search index document by loading the graph element for the given uuid and type and transforming it to a source map which will be used to
	 * update the matching search index document.
	 * 
	 * @param uuid
	 * @param type
	 * @return
	 */
	Observable<Void> update(String uuid, String type);

	/**
	 * Reindex all documents for the type which the handler is capable of.
	 * 
	 * @return
	 */
	Observable<Void> reindexAll();
}
