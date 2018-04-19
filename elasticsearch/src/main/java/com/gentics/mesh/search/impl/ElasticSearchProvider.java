package com.gentics.mesh.search.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.isConflictError;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.isNotFoundError;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.isResourceAlreadyExistsError;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.search.ElasticsearchProcessManager;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import joptsimple.internal.Strings;
import net.lingala.zip4j.exception.ZipException;

/**
 * Elastic search provider class which implements the {@link SearchProvider} interface.
 */
@Singleton
public class ElasticSearchProvider implements SearchProvider {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);

	private SearchClient client;

	private MeshOptions options;

	private Lazy<IndexHandlerRegistry> registry;

	private ElasticsearchProcessManager processManager;

	private final static int MAX_RETRY_ON_ERROR = 5;

	@Inject
	public ElasticSearchProvider(Lazy<IndexHandlerRegistry> registry) {
		this.registry = registry;
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

		if (searchOptions.isStartEmbedded()) {
			try {
				processManager.start();
				processManager.startWatchDog();
			} catch (IOException | ZipException e) {
				log.error("Error while starting embedded Elasticsearch server.", e);
			}
		}

		try {
			URL url = new URL(searchOptions.getUrl());
			int port = url.getPort();
			String proto = url.getProtocol();
			if ("http".equals(proto) && port == -1) {
				port = 80;
			}
			if ("https".equals(proto) && port == -1) {
				port = 443;
			}
			client = new SearchClient(proto, url.getHost(), port);

			if (waitForCluster) {
				waitForCluster(client, searchOptions.getStartupTimeout());
				if (log.isDebugEnabled()) {
					log.debug("Waited for elasticsearch shard: " + (System.currentTimeMillis() - start) + "[ms]");
				}
			}
		} catch (MalformedURLException e) {
			throw error(INTERNAL_SERVER_ERROR, "Invalid search provider url");
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
				JsonObject response = client.clusterHealth().sync();
				String status = response.getString("status");
				log.debug("Elasticsearch status is: " + status);
				if (!"red".equals(status)) {
					log.info("Elasticsearch is ready. Releasing lock after " + (System.currentTimeMillis() - start) + " ms");
					return;
				}
			} catch (HttpErrorException e1) {
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
		processManager = new ElasticsearchProcessManager(Mesh.mesh().getVertx(), options.getSearchOptions());
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
		return client.readIndex("_all").async()
			.flatMapObservable(response -> {
				List<String> indices = Collections.emptyList();
				indices = response.fieldNames().stream().filter(e -> e.startsWith(INDEX_PREFIX)).collect(Collectors.toList());
				if (indices.isEmpty()) {
					log.debug("No indices with prefix {" + INDEX_PREFIX + "} were found.");
				} else {
					if (log.isDebugEnabled()) {
						for (String idx : indices) {
							log.debug("Found index {" + idx + "}");
						}
					}
				}
				return Observable.fromIterable(indices);
			}).flatMapCompletable(index -> {
				// Now delete the found indices
				log.debug("Deleting index {" + index + "}");
				return deleteIndex(index).compose(withTimeoutAndLog("Deleting mesh index {" + index + "}", true));
			}).compose(withTimeoutAndLog("Clearing mesh indices failed", true));

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
		if (indices.length == 0) {
			return client.refresh().async()
				.doOnError(error -> {
					log.error("Refreshing of all indices failed.", error);
					throw error(INTERNAL_SERVER_ERROR, "search_error_refresh_failed", error);
				}).toCompletable()
				.compose(withTimeoutAndLog("Refreshing all indices", true));
		}
		return Observable.fromArray(indices).flatMapCompletable(index -> {
			return client.refresh(index).async()
				.doOnError(error -> {
					log.error("Refreshing of indices {" + index + "} failed.", error);
					throw error(INTERNAL_SERVER_ERROR, "search_error_refresh_failed", error);
				}).toCompletable()
				.compose(withTimeoutAndLog("Refreshing indices {" + index + "}", true));
		});
	}

	@Override
	public Completable createIndex(IndexInfo info) {
		String indexName = info.getIndexName();

		if (log.isDebugEnabled()) {
			log.debug("Creating ES Index {" + indexName + "}");
		}

		JsonObject json = createIndexSettings(info);
		return client.createIndex(indexName, json).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Create index {" + indexName + "}response: {" + response.toString() + "}");
				}
			}).toCompletable()
			.onErrorResumeNext(error -> isResourceAlreadyExistsError(error) ? Completable.complete() : Completable.error(error))
			.compose(withTimeoutAndLog("Creating index {" + indexName + "}", true));
	}

	@Override
	public Single<JsonObject> getDocument(String index, String uuid) {
		return client.getDocument(index, DEFAULT_TYPE, uuid).async()
			.map(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Get object {" + uuid + "} from index {" + index + "}");
				}
				return response;
			}).timeout(getOptions().getTimeout(), TimeUnit.MILLISECONDS).doOnError(error -> {
				log.error("Could not get object {" + uuid + "} from index {" + index + "}", error);
			});
	}

	@Override
	public Completable deleteDocument(String index, String uuid) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting document {" + uuid + "} from index {" + index + "}.");
		}
		return client.deleteDocument(index, DEFAULT_TYPE, uuid).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Deleted object {" + uuid + "} from index {" + index + "}");
				}
			}).toCompletable()
			.onErrorResumeNext(error -> isNotFoundError(error) ? Completable.complete() : Completable.error(error))
			.compose(withTimeoutAndLog("Deleting document {" + index + "} / {" + uuid + "}", true));
	}

	@Override
	public Completable updateDocument(String index, String uuid, JsonObject document, boolean ignoreMissingDocumentError) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Updating object {" + uuid + ":" + DEFAULT_TYPE + "} to index.");
		}

		return client.updateDocument(index, DEFAULT_TYPE, uuid, new JsonObject().put("doc", document)).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug(
						"Update object {" + uuid + ":" + DEFAULT_TYPE + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
			}).toCompletable().onErrorResumeNext(error -> {
				if (ignoreMissingDocumentError && isNotFoundError(error)) {
					return Completable.complete();
				}
				return Completable.error(error);
			}).compose(withTimeoutAndLog("Updating document {" + index + "} / {" + uuid + "}", true));
	}

	@Override
	public Completable processBulk(List<? extends BulkEntry> entries) {
		if (entries.isEmpty()) {
			return Completable.complete();
		}
		long start = System.currentTimeMillis();

		String bulkData = entries.stream().map(BulkEntry::toBulkString).collect(Collectors.joining("\n")) + "\n";
		if (log.isTraceEnabled()) {
			log.trace("Using bulk payload:");
			log.trace(bulkData);
		}
		return client.processBulk(bulkData).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Finished bulk request. Duration " + (System.currentTimeMillis()
						- start) + "[ms]");
				}
			}).toCompletable()
			.compose(withTimeoutAndLog("Storing document batch.", true));
	}

	@Override
	public Completable storeDocument(String index, String uuid, JsonObject document) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Adding object {" + uuid + ":" + DEFAULT_TYPE + "} to index {" + index + "}");
		}
		return client.storeDocument(index, DEFAULT_TYPE, uuid, document).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Added object {" + uuid + ":" + DEFAULT_TYPE + "} to index {" + index + "}. Duration " + (System.currentTimeMillis()
						- start) + "[ms]");
				}
			}).toCompletable().compose(withTimeoutAndLog("Storing document {" + index + "} / {" + uuid + "}", true));
	}

	@Override
	public Completable deleteIndex(boolean failOnMissingIndex, String... indexNames) {
		String indices = Strings.join(indexNames, ",");
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Deleting indices {" + indices + "}");
		}
		return client.deleteIndex(indexNames).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Deleted index {" + indices + "}. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
			}).toCompletable()
			.onErrorResumeNext(error -> isNotFoundError(error) ? Completable.complete() : Completable.error(error))
			.compose(withTimeoutAndLog("Deletion of indices " + indices, true));
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

		return client.createIndexTemplate(templateName, json).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Created template {" + templateName + "} response: {" + response.toString() + "}");
				}
			}).onErrorResumeNext(error -> {
				if (error instanceof HttpErrorException) {
					HttpErrorException re = (HttpErrorException) error;
					JsonObject errorInfo = re.getBodyObject(JsonObject::new);
					String reason = errorInfo.getJsonObject("error").getString("reason");
					return Single.error(error(BAD_REQUEST, "schema_error_index_validation", reason));
				} else {
					return Single.error(error(BAD_REQUEST, "schema_error_index_validation", error.getMessage()));
				}
			}).toCompletable()
			.andThen(client.deleteIndexTemplate(templateName).async().toCompletable())
			.compose(withTimeoutAndLog("Template validation", false));
	}

	@Override
	public String getVendorName() {
		return "elasticsearch";
	}

	@Override
	public String getVersion() {
		try {
			JsonObject info = client.info().sync();
			return info.getJsonObject("version").getString("number");
		} catch (HttpErrorException e) {
			log.error("Unable to fetch node information.", e);
			throw error(INTERNAL_SERVER_ERROR, "Error while fetching version info from elasticsearch.");
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getClient() {
		return (T) client;
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
	 * Modify the given completable and add the configured timeout, error logging and retry handler.
	 * 
	 * @param msg
	 *            Message to be shown on timeout
	 * @param ignoreError
	 *            Whether to ignore any encountered errors
	 * @return
	 */
	private CompletableTransformer withTimeoutAndLog(String msg, boolean ignoreError) {
		Long timeout = getOptions().getTimeout();
		return c -> {
			Completable t = c
				// Retry the operation if an conflict error was returned
				.retry((nTry, error) -> nTry < MAX_RETRY_ON_ERROR && isConflictError(error))
				.timeout(timeout, TimeUnit.MILLISECONDS)
				.doOnError(error -> {
					if (error instanceof TimeoutException) {
						log.error("The operation failed since the timeout of {" + timeout + "} ms has been reached. Action: " + msg);
					} else {
						error.printStackTrace();
						log.error("Request failed {" + msg + "}", error);
					}
				});
			return ignoreError ? t.onErrorComplete() : t;
		};
	}

}
