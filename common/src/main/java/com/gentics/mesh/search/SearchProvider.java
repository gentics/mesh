package com.gentics.mesh.search;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;

import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.json.JsonObject;
import rx.Completable;
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
	 * Update the document.
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
	 * Delete the given document.
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
	 * Store the given document.
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
	 * Store a batch of document.
	 * 
	 * @param index
	 *            Index name
	 * @param type
	 *            Index type
	 * @param documents
	 *            Map of documents in which the key represents the documentId to be used
	 * @return
	 */
	Completable storeDocumentBatch(String index, String type, Map<String, JsonObject> documents);

	/**
	 * Get the given document.
	 * 
	 * @param indexName
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid for the document
	 */
	Single<Map<String, Object>> getDocument(String indexName, String type, String uuid);

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
	 * @return Elasticsearch node
	 */
	// TODO get rid of the elastic search dependency within the interface
	Object getNode();

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
	 * @param query
	 *            Search query
	 * @param indices
	 *            Indices to be searched for documents
	 * @return Single which emits the amount of deleted documents
	 */
	Single<Integer> deleteDocumentsViaQuery(String query, String... indices);

	/**
	 * Delete all documents which were found using the query.
	 * 
	 * @param query
	 *            Search query
	 * @param indices
	 *            Indices to be searched for documents
	 * @return Single which emits the amount of deleted nodes
	 */
	default Single<Integer> deleteDocumentsViaQuery(JSONObject query, String... indices) {
		return deleteDocumentsViaQuery(query.toString(), indices);
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

	/**
	 * Initialise and start the search provider using the given options.
	 * 
	 * @param options
	 * @return Fluent API
	 */
	SearchProvider init(MeshOptions options);

	/**
	 * Update the mapping for the given index and type using the provided mapping json.
	 * 
	 * @param indexName
	 * @param type
	 * @param mapping
	 * @return
	 */
	Completable updateMapping(String indexName, String type, JsonObject mapping);

}
