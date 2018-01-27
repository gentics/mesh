package com.gentics.mesh.search.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.search.ElasticsearchProcessManager;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.lingala.zip4j.exception.ZipException;

/**
 * Elastic search provider class which implements the {@link SearchProvider} interface.
 */
@Singleton
public class ElasticSearchProvider implements SearchProvider {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);

	private SearchClient client;

	private MeshOptions options;

	private ElasticsearchProcessManager processManager;

	public ElasticSearchProvider() {

	}

	@Override
	public void start() {
		start(true);
	}

	/**
	 * Start the provider by creating the REST client and checking the server status.
	 * 
	 * @param waitForCluster
	 */
	public void start(boolean waitForCluster) {
		log.debug("Creating elasticsearch provider.");

		ElasticSearchOptions searchOptions = getOptions();
		long start = System.currentTimeMillis();

		if (searchOptions.isStartEmbeddedES()) {
			try {
				processManager.start();
				processManager.startWatchDog();
			} catch (IOException | ZipException e) {
				log.error("Error while starting embedded Elasticsearch server.", e);
			}
		}

		List<HttpHost> hosts = searchOptions.getHosts().stream().map(hostConfig -> {
			return new HttpHost(hostConfig.getHostname(), hostConfig.getPort(), hostConfig.getProtocol());
		}).collect(Collectors.toList());

		// TODO add support for multiple servers
		HttpHost first = hosts.get(0);
		client = new SearchClient(first.getSchemeName(), first.getHostName(), first.getPort());

		if (waitForCluster) {
			waitForCluster(client, 45);
			if (log.isDebugEnabled()) {
				log.debug("Waited for elasticsearch shard: " + (System.currentTimeMillis() - start) + "[ms]");
			}
		}
	}

	private void waitForCluster(SearchClient client, long timeoutInSec) {
		long start = System.currentTimeMillis();
		// Wait until the cluster is ready
		while (true) {
			if ((System.currentTimeMillis() - start) / 1000 > timeoutInSec) {
				log.debug("Timeout of {" + timeoutInSec + "} reached.");
				break;
			}
			try {
				log.debug("Checking elasticsearch status...");
				JsonObject response = client.info();
				boolean ready = response.getBoolean("todo");
				log.debug("Elasticsearch status is: " + ready);
				if (ready) {
					log.info("Elasticsearch is ready. Releasing lock after " + (System.currentTimeMillis() - start) + " ms");
					return;
				}
			} catch (IOException e1) {
				// ignored
			}
			try {
				log.info("Waiting for elasticsearch status...");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		throw new RuntimeException("Elasticsearch was not ready within set timeout of {" + timeoutInSec + "} seconds.");

	}

	@Override
	public ElasticSearchProvider init(MeshOptions options) {
		this.options = options;
		processManager = new ElasticsearchProcessManager(Mesh.mesh().getVertx());
		return this;
	}

	@Override
	public void reset() {
		if (log.isDebugEnabled()) {
			log.debug("Resetting elastic search");
		}
		try {
			stop();
		} catch (IOException e) {
			e.printStackTrace();
		}
		start();
	}

	/**
	 * Returns the default index settings.
	 * 
	 * @return
	 */
	@Override
	public JsonObject getDefaultIndexSettings() {

		JsonObject tokenizer = new JsonObject();
		tokenizer.put("type", "nGram");
		tokenizer.put("min_gram", "3");
		tokenizer.put("max_gram", "3");

		JsonObject trigramsAnalyzer = new JsonObject();
		trigramsAnalyzer.put("tokenizer", "mesh_default_ngram_tokenizer");
		trigramsAnalyzer.put("filter", new JsonArray().add("lowercase"));

		JsonObject analysis = new JsonObject();
		analysis.put("analyzer", new JsonObject().put("trigrams", trigramsAnalyzer));
		analysis.put("tokenizer", new JsonObject().put("mesh_default_ngram_tokenizer", tokenizer));
		return new JsonObject().put("analysis", analysis);

	}

	@Override
	public Completable clear() {

		// Read all indices and locate indices which have been created for/by mesh.
		Maybe<List<String>> indexInfo = client.readIndexAsync("_all").flatMapMaybe(response -> {
			List<String> indices = Collections.emptyList();
			indices = response.fieldNames().stream().filter(e -> e.startsWith(INDEX_PREFIX)).collect(Collectors.toList());
			if (indices.isEmpty()) {
				return Maybe.empty();
			} else {
				return Maybe.just(indices);
			}
		});

		// Now delete the found indices
		return indexInfo.flatMapCompletable(result -> {
			log.debug("Deleting indices {" + StringUtils.join(result.toArray(), ","));
			String[] indices = result.toArray(new String[result.size()]);
			return deleteIndex(indices);
		}).compose(withTimeoutAndLog("Clearing mesh indices."));
	}

	@Override
	public void stop() throws IOException {
		if (client != null) {
			log.info("Closing Elasticsearch REST client.");
			client.close();
		}

		if (processManager != null) {
			log.info("Stopping Elasticsearch server.");
			processManager.stopWatchDog();
			processManager.stop();
		}
	}

	@Override
	public Completable refreshIndex(String... indices) {
		String indicesStr = StringUtils.join(indices, ",");

		String path = "/_refresh";
		if (indices.length > 0) {
			path = "/" + indicesStr + "/_refresh";
		}

		return client.refreshAsync(indices).doOnError(error -> {
			log.error("Refreshing of indices {" + indicesStr + "} failed.", error);
			throw error(INTERNAL_SERVER_ERROR, "search_error_refresh_failed", error);
		}).toCompletable().compose(withTimeoutAndLog("Refreshing indices {" + indicesStr + "}"));
	}

	@Override
	public Completable createIndex(IndexInfo info) {
		String indexName = info.getIndexName();

		if (log.isDebugEnabled()) {
			log.debug("Creating ES Index {" + indexName + "}");
		}

		JsonObject json = createIndexSettings(info);
		return client.createIndexAsync(indexName, json).doOnSuccess(response -> {
			if (log.isDebugEnabled()) {
				log.debug("Create index {" + indexName + "}response: {" + response.toString() + "}");
			}

		}).doOnError(error -> {
			if (error instanceof HttpErrorException) {
				HttpErrorException re = (HttpErrorException) error;
				JsonObject ob = re.getBodyObject();
				String type = ob.getJsonObject("error").getString("type");
				if (type.equals("resource_already_exists_exception")) {
					sub.onComplete();
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Got failure response {" + ob.encodePrettily() + "}");
					}
				}

			} else {
				log.error("Error while creating index {" + indexName + "}", error);
			}

		}).toCompletable().compose(withTimeoutAndLog("Creating index {" + indexName + "}"));
	}

	@Override
	public Single<Map<String, Object>> getDocument(String index, String uuid) {

		Single<Map<String, Object>> s = client.getDocumentAsync(index, DEFAULT_TYPE, uuid).map(response -> {
			if (log.isDebugEnabled()) {
				log.debug("Get object {" + uuid + "} from index {" + index + "}");
			}
			// return response.getSourceAsMap();
			return new HashMap<String, Object>();
		});
		return s.doOnError(error -> {
			log.error("Could not get object {" + uuid + "} from index {" + index + "}", error);
		}).timeout(getOptions().getTimeout(), TimeUnit.MILLISECONDS).doOnError(log::error);

	}

	@Override
	public Completable deleteDocument(String index, String uuid) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting document {" + uuid + "} from index {" + index + "}.");
		}
		return client.deleteDocumentAsync(index, DEFAULT_TYPE, uuid).doOnSuccess(response -> {
			if (log.isDebugEnabled()) {
				log.debug("Deleted object {" + uuid + "} from index {" + index + "}");
			}
		}).doOnError(error -> {
			if (error instanceof HttpErrorException) {
				HttpErrorException se = (HttpErrorException) error;
				// if (e instanceof DocumentMissingException) {
				sub.onComplete();
				return;
			} else {
				log.error("Could not delete object {" + uuid + "} from index {" + index + "}");
			}
		}).toCompletable().compose(withTimeoutAndLog("Deleting document {" + index + "} / {" + uuid + "}"));
	}

	@Override
	public Completable updateDocument(String index, String uuid, JsonObject document, boolean ignoreMissingDocumentError) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Updating object {" + uuid + ":" + DEFAULT_TYPE + "} to index.");
		}

		return client.updateDocumentAsync(index, DEFAULT_TYPE, uuid, document).doOnSuccess(response -> {
			if (log.isDebugEnabled()) {
				log.debug("Update object {" + uuid + ":" + DEFAULT_TYPE + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
			}

		}).doOnError(error -> {
			if (error instanceof HttpErrorException) {
				HttpErrorException se = (HttpErrorException) error;
				if (ignoreMissingDocumentError && se.getStatusCode() == 404) {
					sub.onComplete();
					return;
				}
			}
			log.error(
					"Updating object {" + uuid + ":" + DEFAULT_TYPE + "} to index failed. Duration " + (System.currentTimeMillis() - start) + "[ms]",
					error);

		}).toCompletable().compose(withTimeoutAndLog("Updating document {" + index + "} / {" + uuid + "}"));
	}

	@Override
	public Completable storeDocumentBatch(String index, Map<String, JsonObject> documents) {
		if (documents.isEmpty()) {
			return Completable.complete();
		}
		long start = System.currentTimeMillis();

		JsonArray items = new JsonArray();
		JsonObject bulkData = new JsonObject();
		bulkData.put("items", items);

		// Add index requests for each document to the bulk request
		for (Map.Entry<String, JsonObject> entry : documents.entrySet()) {
			JsonObject item = new JsonObject();
			// item.put("index", value)
			// String documentId = entry.getKey();
			// JsonObject document = entry.getValue();
			// request.add(new IndexRequest(index, DEFAULT_TYPE, documentId).source(document.toString(), XContentType.JSON));
			items.add(item);
		}

		return client.storeDocumentBulkAsync(bulkData).doOnSuccess(response -> {
			if (log.isDebugEnabled()) {
				log.debug("Finished bulk  store request on index {" + index + ":" + DEFAULT_TYPE + "}. Duration "
						+ (System.currentTimeMillis() - start) + "[ms]");
			}
		}).doOnError(error -> {
			log.error("Bulk store on index {" + index + ":" + DEFAULT_TYPE + "} to index failed. Duration " + (System.currentTimeMillis() - start)
					+ "[ms]", error);
		}).toCompletable().compose(withTimeoutAndLog("Storing document batch"));
	}

	@Override
	public Completable storeDocument(String index, String uuid, JsonObject document) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Adding object {" + uuid + ":" + DEFAULT_TYPE + "} to index {" + index + "}");
		}
		client.storeDocumentAsync(index, DEFAULT_TYPE, uuid, document).doOnSuccess(response -> {
			if (log.isDebugEnabled()) {
				log.debug("Added object {" + uuid + ":" + DEFAULT_TYPE + "} to index {" + index + "}. Duration "
						+ (System.currentTimeMillis() - start) + "[ms]");
			}

		}).doOnError(error -> {
			log.error("Adding object {" + uuid + ":" + DEFAULT_TYPE + "} to index {" + index + "} failed. Duration "
					+ (System.currentTimeMillis() - start) + "[ms]", error);

		}).toCompletable().compose(withTimeoutAndLog("Storing document {" + index + "} / {" + uuid + "}"));
	}

	@Override
	public Completable deleteIndex(boolean failOnMissingIndex, String... indexNames) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Deleting index {" + indexNames + "}");
		}
		client.deleteIndexAsync(indexNames).doOnSuccess(response -> {
			if (log.isDebugEnabled()) {
				log.debug("Deleted index {" + indexNames + "}. Duration " + (System.currentTimeMillis() - start) + "[ms]");
			}
		}).doOnError(error -> {
			if (error instanceof HttpErrorException) {
				HttpErrorException se = (HttpErrorException) error;
				if (se.getStatusCode() == 404) {
					sub.onComplete();
					return;
				}
			} else {
				log.error("Deleting index {" + indexNames + "} failed. Duration " + (System.currentTimeMillis() - start) + "[ms]", error);
			}

		}).toCompletable().compose(withTimeoutAndLog("Deletion of index " + indexNames));
	}

	@Override
	public Completable validateCreateViaTemplate(IndexInfo info) {
		JsonObject json = createIndexSettings(info);
		if (log.isDebugEnabled()) {
			log.debug("Validating index configuration {" + json.encodePrettily() + "}");
		}

		String randomName = info.getIndexName() + UUIDUtil.randomUUID();
		String templateName = randomName.toLowerCase();
		json.put("template", templateName);

		client.createIndexTemplateAsync(templateName, json).doOnSuccess(response -> {
			if (log.isDebugEnabled()) {
				log.debug("Created template {" + templateName + "} response: {" + response.toString() + "}");
			}
		}).doOnError(error -> {
			if (error instanceof HttpErrorException) {
				HttpErrorException re = (HttpErrorException) error;
				JsonObject errorInfo = re.getBodyObject();
				throw error(BAD_REQUEST, "schema_error_index_validation", errorInfo.getJsonObject("error").getString("reason"));
			} else {
				throw error(BAD_REQUEST, "schema_error_index_validation", error.getMessage());
			}

		}).toCompletable().andThen(client.deleteIndexTemplateAsync(templateName).toCompletable()).compose(withTimeoutAndLog("Template validation"));
	}

	@Override
	public String getVendorName() {
		return "elasticsearch";
	}

	@Override
	public String getVersion() {
		try {
			JsonObject info = client.info();
			return info.getString("version");
		} catch (IOException e) {
			log.error("Unable to fetch node information.", e);
			throw error(INTERNAL_SERVER_ERROR, "Error while fetching version info from elasticsearch.");
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getClient() {
		return (T) client;
	}

	private SearchClient getSearchClient() {
		return client;
	}

	/**
	 * Return the elasticsearch options.
	 * 
	 * @return
	 */
	public ElasticSearchOptions getOptions() {
		return options.getSearchOptions();
	}

	/**
	 * Modify the given completable and add the configured timeout and error logging.
	 * 
	 * @param c
	 * @return
	 */
	private CompletableTransformer withTimeoutAndLog(String msg) {
		Long timeout = getOptions().getTimeout();
		return c -> c.timeout(timeout, TimeUnit.MILLISECONDS).doOnError(error -> {
			if (error instanceof TimeoutException) {
				log.error("The operation failed since the timeout of {" + timeout + "} ms has been reached. Action: " + msg);
			} else {
				log.error(error);
			}
		}).onErrorComplete();
	}

}
