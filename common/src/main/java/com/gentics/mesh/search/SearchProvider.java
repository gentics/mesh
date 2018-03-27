package com.gentics.mesh.search;

import java.io.IOException;
import java.util.Map;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

/**
 * A search provider is a service this enables storage and retrieval of indexed documents.
 */
public interface SearchProvider {

	/**
	 * Prefix for indices created by mesh.
	 */
	static final String INDEX_PREFIX = "mesh-";

	/**
	 * Default document type for all indices. Note that the type handling will be removed in future ES versions.
	 */
	static final String DEFAULT_TYPE = "default";

	/**
	 * Explicitly refresh one or more indices (making the content indexed since the last refresh searchable).
	 * 
	 * @param indices
	 *            Indices to refresh
	 * @return Completable for the action
	 */
	Completable refreshIndex(String... indices);

	/**
	 * Create a search index with index information.
	 * 
	 * @param info
	 *            Index information which includes index name, mappings and settings.
	 * @return Completable for the action
	 */
	Completable createIndex(IndexInfo info);

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
	 * @return Single that yields the document data
	 */
	Single<JsonObject> getDocument(String indexName, String uuid);

	/**
	 * Start the search provider.
	 */
	void start();

	/**
	 * Stop the search provider.
	 * 
	 * @throws IOException
	 */
	void stop() throws IOException;

	/**
	 * Reset the search provider.
	 */
	void reset();

	/**
	 * Delete all indices which are managed by mesh.
	 * 
	 * @return Completable for the clear action
	 */
	Completable clear();

	/**
	 * Delete the given index and don't fail if the index is not existing.
	 * 
	 * @param indexName
	 *            Name of the index which should be deleted
	 * @return
	 */
	default Completable deleteIndex(String... indexName) {
		return deleteIndex(false, indexName);
	}

	/**
	 * Delete the given indices.
	 * 
	 * @param failOnMissingIndex
	 * @param indexNames
	 *            Names of the indices which should be deleted
	 * @return
	 */
	Completable deleteIndex(boolean failOnMissingIndex, String... indexNames);

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
	 * Create the index settings and use the given index information in order to extend the default settings.
	 * 
	 * @param info
	 * @return
	 */
	default JsonObject createIndexSettings(IndexInfo info) {
		JsonObject settings = info.getIndexSettings();
		JsonObject mappings = info.getIndexMappings();
		// Prepare the json for the request
		JsonObject json = new JsonObject();
		JsonObject fullSettings = new JsonObject();
		fullSettings.mergeIn(getDefaultIndexSettings(), true);
		if (settings != null) {
			fullSettings.mergeIn(settings, true);
		}
		json.put("settings", fullSettings);
		json.put("mappings", mappings);
		return json;
	}

	/**
	 * Validate the syntax of the provided information by creating a template.
	 * 
	 * @param info
	 * @return
	 */
	Completable validateCreateViaTemplate(IndexInfo info);

}
