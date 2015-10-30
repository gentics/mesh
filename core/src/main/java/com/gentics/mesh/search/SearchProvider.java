package com.gentics.mesh.search;

import java.util.Map;

import org.elasticsearch.node.Node;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A search provider is a service this enables storage and retrieval of indexed documents.
 */
public interface SearchProvider {

	/**
	 * Explicitly refresh one or more indices (making the content indexed since the last refresh searchable).
	 */
	void refreshIndex();

	//	void createIndex(String indexName, String type);

	// TODO add a good response instead of void. We need this in oder to handle correct logging?
	/**
	 * Update the document and invoke the handler when the document has been updated or an error occurred.
	 * 
	 * @param index
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid of the document
	 * @param transformToDocumentMap
	 * @param handler
	 *            Completion handler
	 */
	void updateDocument(String index, String type, String uuid, Map<String, Object> transformToDocumentMap, Handler<AsyncResult<Void>> handler);

	/**
	 * Delete the given document and invoke the handler when the document has been deleted or an error occurred.
	 * 
	 * @param index
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid for the document
	 * @param handler
	 *            Completion handler
	 */
	void deleteDocument(String index, String type, String uuid, Handler<AsyncResult<Void>> handler);

	/**
	 * Store the given document and invoke the handler when the document has been stored or an error occurred.
	 * 
	 * @param index
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid for the document
	 * @param map
	 *            Map that holds the document properties
	 * @param handler
	 *            Completion handler
	 */
	void storeDocument(String index, String type, String uuid, Map<String, Object> map, Handler<AsyncResult<Void>> handler);

	/**
	 * Get the given document and invoke the handler when the document has been loaded or an error occurred.
	 * 
	 * @param index
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid for the document
	 * @param handler
	 *            Completion handler
	 */
	void getDocument(String index, String type, String uuid, Handler<AsyncResult<Map<String, Object>>> handler);

	/**
	 * Start the search provider.
	 */
	void start();

	/**
	 * Stop the search provider.
	 */
	void stop();

	/**
	 * Reset the search provider.
	 */
	void reset();

	/**
	 * Return the elastic search node.
	 * 
	 * @return
	 */
	//TODO get rid of the elastic search dependency within the interface
	Node getNode();

}
