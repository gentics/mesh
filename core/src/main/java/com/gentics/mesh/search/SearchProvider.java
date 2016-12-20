package com.gentics.mesh.search;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;

import io.vertx.core.json.JsonObject;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * A search provider is a service this enables storage and retrieval of indexed documents.
 */
public interface SearchProvider {

	/**
	 * Explicitly refresh one or more indices (making the content indexed since the last refresh searchable).
	 * 
	 * @param indices
	 */
	void refreshIndex(String... indices);

	/**
	 * Create a search index with the given name.
	 * 
	 * @param indexName
	 */
	Completable createIndex(String indexName);

	// TODO add a good response instead of void. We need this in order to handle correct logging?
	/**
	 * Update the document and invoke the handler when the document has been updated or an error occurred.
	 * 
	 * @param indexName
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid of the document
	 * @param document
	 *            Document which should be stored
	 * 
	 */
	Completable updateDocument(String indexName, String type, String uuid, JsonObject document);

	/**
	 * Delete the given document and invoke the handler when the document has been deleted or an error occurred.
	 * 
	 * @param indexName
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid for the document
	 */
	Completable deleteDocument(String indexName, String type, String uuid);

	/**
	 * Store the given document and invoke the handler when the document has been stored or an error occurred.
	 * 
	 * @param indexName
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid for the document
	 * @param document
	 *            JSON Object which holds the document data
	 */
	Completable storeDocument(String indexName, String type, String uuid, JsonObject document);

	/**
	 * Get the given document and invoke the handler when the document has been loaded or an error occurred.
	 * 
	 * @param indexName
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid for the document
	 */
	Observable<Map<String, Object>> getDocument(String indexName, String type, String uuid);

	/**
	 * Start the search provider.
	 * @throws NodeValidationException 
	 */
	void start() throws NodeValidationException;

	/**
	 * Stop the search provider.
	 * @throws IOException 
	 */
	void stop() throws IOException;

	/**
	 * Reset the search provider.
	 * @throws IOException 
	 * @throws NodeValidationException 
	 */
	void reset() throws IOException, NodeValidationException;

	/**
	 * Return the elastic search node.
	 * 
	 * @return Elasticsearch node
	 */
	// TODO get rid of the elastic search dependency within the interface
	Node getNode();

	/**
	 * Clear the given index. This will effectively remove all documents from the index without removing the index itself.
	 * 
	 * @param indexName
	 */
	Completable clearIndex(String indexName);

	/**
	 * Delete all indices.
	 */
	void clear();

	/**
	 * Delete the given index.
	 * 
	 * @param indexName
	 * @return
	 */
	Completable deleteIndex(String indexName);

	/**
	 * Delete all documents which were found using the query.
	 * 
	 * @param indexName
	 *            Index to be searched for documents
	 * @param query
	 *            Search query
	 * @return Observable which emits the amount of deleted documents
	 */
	Single<Integer> deleteDocumentsViaQuery(String indexName, String query);

	/**
	 * Delete all documents which were found using the query.
	 * 
	 * @param indexName
	 *            Index to be searched for documents
	 * @param query
	 *            Search query
	 * @return Observable which emits the amount of deleted nodes
	 */
	default Single<Integer> deleteDocumentsViaQuery(String indexName, JSONObject query) {
		return deleteDocumentsViaQuery(indexName, query.toString());
	}

	/**
	 * Returns the search provider vendor name.
	 * 
	 * @return
	 */
	String getVendorName();

	/**
	 * Returns the version of the used search engine.
	 * 
	 * @return
	 */
	String getVersion();

}
