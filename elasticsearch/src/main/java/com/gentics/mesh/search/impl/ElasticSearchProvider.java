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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.search.ElasticsearchProcessManager;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.core.Vertx;
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

	private static final String INGEST_PIPELINE_PLUGIN_NAME = "ingest-attachment";

	private SearchClient client;

	private MeshOptions options;

	private ElasticsearchProcessManager processManager;

	private final static int MAX_RETRY_ON_ERROR = 5;

	private Set<String> registerdPlugins = new HashSet<>();

	private Lazy<Vertx> vertx;

	private Function<Throwable, CompletableSource> ignore404 = error -> isNotFoundError(error) ? Completable.complete()
		: Completable.error(error);

	@Inject
	public ElasticSearchProvider(Lazy<Vertx> vertx, MeshOptions options) {
		this.vertx = vertx;
		this.options = options;
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
				registerdPlugins = loadPluginInfo().blockingGet();
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
	public ElasticSearchProvider init() {
		processManager = new ElasticsearchProcessManager(vertx.get(), options.getSearchOptions());
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
		String prefix = installationPrefix();
		// Read all indices and locate indices which have been created for/by mesh.
		Completable clearIndices = client.readIndex("_all").async()
			.flatMapObservable(response -> {
				List<String> indices = response.fieldNames()
					.stream()
					.filter(e -> e.startsWith(prefix))
					.map(this::removePrefix)
					.collect(Collectors.toList());
				if (indices.isEmpty()) {
					log.debug("No indices with prefix {" + prefix + "} were found.");
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

		// Read all pipelines and remove them
		Completable clearPipelines = client.listPipelines().async()
			.onErrorResumeNext(error -> isNotFoundError(error) ? Single.just(new JsonObject()) : Single.error(error))
			.flatMapObservable(response -> {
				List<String> meshPipelines = response.fieldNames()
					.stream()
					.filter(e -> e.startsWith(prefix))
					.map(this::removePrefix)
					.collect(Collectors.toList());
				if (meshPipelines.isEmpty()) {
					log.debug("No pipelines with prefix {" + prefix + "} were found");
				}
				return Observable.fromIterable(meshPipelines);
			}).flatMapCompletable(pipeline -> {
				log.debug("Deleting pipeline {" + pipeline + "}");
				return deregisterPipeline(pipeline)
					.compose(withTimeoutAndLog("Deleting pipeline {" + pipeline + "}", true));
			}).compose(withTimeoutAndLog("Clearing mesh piplines failed", true));

		return Completable.mergeArray(clearIndices, clearPipelines);
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
			String fullIndex = installationPrefix() + index;
			return client.refresh(fullIndex).async()
				.doOnError(error -> {
					log.error("Refreshing of indices {" + fullIndex + "} failed.", error);
					throw error(INTERNAL_SERVER_ERROR, "search_error_refresh_failed", error);
				}).toCompletable()
				.compose(withTimeoutAndLog("Refreshing indices {" + fullIndex + "}", true));
		});
	}

	public boolean hasPlugin(String name) {
		Objects.requireNonNull(name, "A valid plugin name must be specified.");
		return registerdPlugins.contains(name);
	}

	@Override
	public Single<Set<String>> loadPluginInfo() {
		return client.nodesInfo().async().map(info -> {
			Set<String> pluginSet = new HashSet<>();
			JsonObject nodes = info.getJsonObject("nodes");
			for (String nodeId : nodes.fieldNames()) {
				JsonObject node = nodes.getJsonObject(nodeId);
				JsonArray plugins = node.getJsonArray("plugins");
				if (plugins == null) {
					continue;
				}
				for (int i = 0; i < plugins.size(); i++) {
					JsonObject plugin = plugins.getJsonObject(i);
					String name = plugin.getString("name");
					pluginSet.add(name);
				}
			}
			return pluginSet;
		});
	}

	@Override
	public Completable createIndex(IndexInfo info) {
		String indexName = installationPrefix() + info.getIndexName();

		if (log.isDebugEnabled()) {
			log.debug("Creating ES Index {" + indexName + "}");
		}

		JsonObject json = createIndexSettings(info);
		Completable indexCreation = client.createIndex(indexName, json).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Create index {" + indexName + "} response: {" + response.toString() + "}");
				}
			}).toCompletable()
			.onErrorResumeNext(error -> isResourceAlreadyExistsError(error) ? Completable.complete() : Completable.error(error))
			.compose(withTimeoutAndLog("Creating index {" + indexName + "}", true));

		if (info.getIngestPipelineSettings() != null && hasIngestPipelinePlugin()) {
			return Completable.mergeArray(indexCreation, registerIngestPipeline(info));
		} else {
			return indexCreation;
		}
	}

	@Override
	public Completable registerIngestPipeline(IndexInfo info) {
		String name = installationPrefix() + info.getIngestPipelineName();
		JsonObject config = info.getIngestPipelineSettings();
		return client.registerPipeline(name, config).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Registered pipeline {" + name + "} response: {" + response.toString() + "}");
				}
			}).toCompletable()
			.onErrorResumeNext(error -> isResourceAlreadyExistsError(error) ? Completable.complete() : Completable.error(error))
			.compose(withTimeoutAndLog("Creating pipeline {" + name + "}", true));
	}

	@Override
	public Single<JsonObject> getDocument(String index, String uuid) {
		String fullIndex = installationPrefix() + index;
		return client.getDocument(fullIndex, DEFAULT_TYPE, uuid).async()
			.map(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Get object {" + uuid + "} from index {" + fullIndex + "}");
				}
				return response;
			}).timeout(getOptions().getTimeout(), TimeUnit.MILLISECONDS).doOnError(error -> {
				log.error("Could not get object {" + uuid + "} from index {" + fullIndex + "}", error);
			});
	}

	@Override
	public Completable deleteDocument(String index, String uuid) {
		String fullIndex = installationPrefix() + index;
		if (log.isDebugEnabled()) {
			log.debug("Deleting document {" + uuid + "} from index {" + fullIndex + "}.");
		}
		return client.deleteDocument(fullIndex, DEFAULT_TYPE, uuid).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Deleted object {" + uuid + "} from index {" + fullIndex + "}");
				}
			}).toCompletable()
			.onErrorResumeNext(ignore404)
			.compose(withTimeoutAndLog("Deleting document {" + fullIndex + "} / {" + uuid + "}", true));
	}

	@Override
	public Completable updateDocument(String index, String uuid, JsonObject document, boolean ignoreMissingDocumentError) {
		String fullIndex = installationPrefix() + index;
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Updating object {" + uuid + ":" + DEFAULT_TYPE + "} to index.");
		}

		return client.updateDocument(fullIndex, DEFAULT_TYPE, uuid, new JsonObject().put("doc", document)).async()
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
			}).compose(withTimeoutAndLog("Updating document {" + fullIndex + "} / {" + uuid + "}", true));
	}

	@Override
	public Completable processBulk(List<? extends BulkEntry> entries) {
		if (entries.isEmpty()) {
			return Completable.complete();
		}
		long start = System.currentTimeMillis();

		String bulkData = entries.stream()
			.map(e -> e.toBulkString(installationPrefix()))
			.collect(Collectors.joining("\n")) + "\n";
		if (log.isTraceEnabled()) {
			log.trace("Using bulk payload:");
			log.trace(bulkData);
		}
		return client.processBulk(bulkData).async()
			.doOnSuccess(response -> {
				boolean errors = response.getBoolean("errors");
				if (errors) {
					JsonArray items = response.getJsonArray("items");
					for (int i = 0; i < items.size(); i++) {
						JsonObject item = items.getJsonObject(i).getJsonObject("index");
						if (item != null && item.containsKey("error")) {
							JsonObject error = item.getJsonObject("error");
							String type = error.getString("type");
							String reason = error.getString("reason");
							String id = item.getString("_id");
							String index = item.getString("_index");
							log.error("Could not store document {" + index + ":" + id + "} - " + type + " : " + reason);
						}
					}
				}

				if (log.isDebugEnabled()) {
					log.debug("Finished bulk request. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
			}).toCompletable()
			.compose(withTimeoutAndLog("Storing document batch.", true));
	}

	@Override
	public Completable storeDocument(String index, String uuid, JsonObject document) {
		String fullIndex = installationPrefix() + index;
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Adding object {" + uuid + ":" + DEFAULT_TYPE + "} to index {" + fullIndex + "}");
		}
		return client.storeDocument(fullIndex, DEFAULT_TYPE, uuid, document).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Added object {" + uuid + ":" + DEFAULT_TYPE + "} to index {" + fullIndex + "}. Duration " + (System.currentTimeMillis()
						- start) + "[ms]");
				}
			}).toCompletable().compose(withTimeoutAndLog("Storing document {" + fullIndex + "} / {" + uuid + "}", true));
	}

	@Override
	public Completable deleteIndex(boolean failOnMissingIndex, String... indexNames) {
		String[] fullIndexNames = Arrays.stream(indexNames).map(i -> installationPrefix() + i).toArray(String[]::new);
		String indices = Strings.join(fullIndexNames, ",");
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Deleting indices {" + indices + "}");
		}
		Completable deleteIndex = client.deleteIndex(fullIndexNames).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Deleted index {" + indices + "}. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
			}).toCompletable()
			.onErrorResumeNext(ignore404)
			.compose(withTimeoutAndLog("Deletion of indices " + indices, true));

		if (hasIngestPipelinePlugin()) {
			Completable deletePipelines = Observable.fromArray(indexNames).flatMapCompletable(indexName -> {
				// We don't need to delete the pipeline for non node indices
				if (!indexName.startsWith("node")) {
					return Completable.complete();
				}
				return deregisterPipeline(indexName);
			})
				.onErrorResumeNext(error -> {
					return isNotFoundError(error) ? Completable.complete() : Completable.error(error);
				})
				.compose(withTimeoutAndLog("Deletion of pipelines " + indices, true));
			return Completable.mergeArray(deleteIndex, deletePipelines);
		} else {
			return deleteIndex;
		}

	}

	@Override
	public Completable deregisterPipeline(String name) {
		String fullname = installationPrefix() + name;
		return client.deregisterPlugin(fullname).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Deregistered pipeline {" + fullname + "} response: {" + response.toString() + "}");
				}
			}).toCompletable()
			.onErrorResumeNext(ignore404)
			.compose(withTimeoutAndLog("Removed pipeline {" + fullname + "}", true));
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
						log.error("Request failed {" + msg + "}", error);
					}
				});
			return ignoreError ? t.onErrorComplete() : t;
		};
	}

	@Override
	public boolean hasIngestPipelinePlugin() {
		return registerdPlugins.contains(INGEST_PIPELINE_PLUGIN_NAME);
	}

	/**
	 * Remove the installation prefix from the given string.
	 * 
	 * @param withPrefix
	 * @return
	 */
	public String removePrefix(String withPrefix) {
		String prefix = installationPrefix();
		if (withPrefix.startsWith(prefix)) {
			return withPrefix.substring(prefix.length());
		} else {
			return withPrefix;
		}
	}

	@Override
	public String installationPrefix() {
		return options.getSearchOptions().getPrefix();
	}

	@Override
	public Single<Boolean> isAvailable() {
		try {
			return client.clusterHealth().async()
				.timeout(1, TimeUnit.SECONDS)
				.map(ignore -> true)
				.onErrorReturnItem(false);
		} catch (HttpErrorException e) {
			return Single.just(false);
		}
	}
}
