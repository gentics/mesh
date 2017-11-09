package com.gentics.mesh.search;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.json.JsonObject;
import rx.Completable;
import rx.Single;

/**
 * A search provider is a service this enables storage and retrieval of indexed documents.
 */
public interface SearchProvider {

	static String DEFAULT_TYPE = "default";

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
	 *            Index name
	 * @param indexSettings
	 *            Index settings (e.g.: tokenizer, analyzer)
	 * @param mapping
	 *            Index mapping
	 */
	Completable createIndex(String indexName, JsonObject indexSettings, JsonObject mapping);

	// TODO add a good response instead of void. We need this in order to handle correct logging?
	/**
	 * Update the document.
	 * 
	 * @param indexName
	 *            Index name of the document
	 * @param uuid
	 *            Uuid of the document
	 * @param document
	 *            Document which should be stored
	 * @param ignoreMissingDocumentError
	 *            Whether to ignore missing document errors
	 */
	Completable updateDocument(String indexName, String uuid, JsonObject document, boolean ignoreMissingDocumentError);

	/**
	 * Delete the given document.
	 * 
	 * @param indexName
	 *            Index name of the document
	 * @param uuid
	 *            Uuid for the document
	 */
	Completable deleteDocument(String indexName, String uuid);

	/**
	 * Store the given document.
	 * 
	 * @param indexName
	 *            Index name of the document
	 * @param uuid
	 *            Uuid for the document
	 * @param document
	 *            JSON Object which holds the document data
	 */
	Completable storeDocument(String indexName, String uuid, JsonObject document);

	/**
	 * Store a batch of document.
	 * 
	 * @param index
	 *            Index name
	 * @param documents
	 *            Map of documents in which the key represents the documentId to be used
	 * @return
	 */
	Completable storeDocumentBatch(String index, Map<String, JsonObject> documents);

	/**
	 * Get the given document.
	 * 
	 * @param indexName
	 *            Index name of the document
	 * @param uuid
	 *            Uuid for the document
	 */
	Single<Map<String, Object>> getDocument(String indexName, String uuid);

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
	 * Delete the given index and don't fail if the index is not existing.
	 * 
	 * @param indexName
	 *            Name of the index which should be deleted
	 * @return
	 */
	default Completable deleteIndex(String indexName) {
		return deleteIndex(indexName, false);
	}

	/**
	 * Delete the given index.
	 * 
	 * @param indexName
	 *            Name of the index which should be deleted
	 * @param failOnMissingIndex
	 * @return
	 */
	Completable deleteIndex(String indexName, boolean failOnMissingIndex);

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

//	/**
//	 * Update the mapping for the given index using the provided mapping json.
//	 * 
//	 * @param indexName
//	 * @param mapping
//	 * @return
//	 */
//	Completable updateMapping(String indexName, JsonObject mapping);

	/**
	 * Return the search provider client.
	 * 
	 * @return
	 */
	<T> T getClient();

	/**
	 * Returns the default index settings.
	 * 
	 * @return
	 */
	JsonObject getDefaultIndexSettings();

	/**
	 * Create the index settings and use the given settings and mappings in order to extend the default settings.
	 * 
	 * @param settings
	 * @param mappings
	 * @return
	 */
	default JsonObject createIndexSettings(JsonObject settings, JsonObject mappings) {
		// Prepare the json for the request
		JsonObject json = new JsonObject();
		json.put("settings", getDefaultIndexSettings());
		json.put("mappings", mappings);
		return json;
	}

	/**
	 * Create the index using the provided information.
	 * 
	 * @param info
	 * @return
	 */
	default Completable createIndex(IndexInfo info) {
		return createIndex(info.getIndexName(), info.getIndexSettings(), info.getIndexMappings());
	}

	/**
	 * Validate the syntax of the provided information by creating a template.
	 * 
	 * @param info
	 * @return
	 */
	Completable validateCreateViaTemplate(IndexInfo info);

}
