package com.gentics.mesh.search;

import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.Bulkable;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A search provider is a service this enables storage and retrieval of indexed documents.
 */
public interface SearchProvider {

	/**
	 * Default document type for all indices. Note that the type handling will be removed in future ES versions.
	 */
	static final String DEFAULT_TYPE = null;

	/**
	 * Explicitly refresh one or more indices (making the content indexed since the last refresh searchable).
	 * 
	 * @param indices
	 *            Indices to refresh
	 * @return Completable for the action
	 */
	Completable refreshIndex(String... indices);

	/**
	 * Load a list of all existing indices.
	 * 
	 * @return
	 */
	Single<Set<String>> listIndices();

	/**
	 * Create a search index with index information.
	 * 
	 * @param info
	 *            Index information which includes index name, mappings and settings.
	 * @return Completable for the action
	 */
	Completable createIndex(IndexInfo info);

	/**
	 * Deregister the ingest pipeline using the index information.
	 * 
	 * @param name
	 * @return Completable for the action
	 */
	Completable deregisterPipeline(String name);

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
	 * Process the bulk request.
	 *
	 * @param actions
	 * @return
	 */
	Completable processBulk(String actions);

	/**
	 * Process the bulk request.
	 * 
	 * @param entries
	 * @return
	 */
	@Deprecated
	Completable processBulkOld(List<? extends BulkEntry> entries);


	/**
	 * Process the bulk request.
	 *
	 * @param entries
	 * @return
	 */
	Completable processBulk(Collection<? extends Bulkable> entries);

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
	 * Initialize and start the search provider.
	 * 
	 * @return Fluent API
	 */
	SearchProvider init();

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

	/**
	 * Return the specific prefix for this installation. Indices and pipelines will make use of this prefix so that multiple mesh instances can use the same
	 * search server.
	 * 
	 * @return
	 */
	String installationPrefix();

	/**
	 * Check if the search provider is ready to process search queries.
	 *
	 * @return
	 */
	Single<Boolean> isAvailable();

	/**
	 * Check whether the provider is active and able to process data.
	 * 
	 * @return
	 */
	boolean isActive();
}
