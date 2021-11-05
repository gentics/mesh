package com.gentics.mesh.search.impl;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CLEAR_FINISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.isConflictError;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.isNotFoundError;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.isResourceAlreadyExistsError;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.Bulkable;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.search.ElasticsearchProcessManager;
import com.gentics.mesh.search.SearchMappingsCache;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.core.Vertx;
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

	private final static int MAX_RETRY_ON_ERROR = 5;

	private final MeshOptions options;

	private final Lazy<Vertx> vertx;

	private final ElasticsearchClient<JsonObject> client;

	private ElasticsearchProcessManager processManager;

	private Function<Throwable, CompletableSource> ignore404 = error -> isNotFoundError(error) ? Completable.complete()
		: Completable.error(error);

	private final ComplianceMode complianceMode;

	private final Lazy<SearchMappingsCache> searchMappingsCache;

	@Inject
	public ElasticSearchProvider(Lazy<Vertx> vertx, MeshOptions options, ElasticsearchClient<JsonObject> client, Lazy<SearchMappingsCache> searchMappingsCache) {
		this.vertx = vertx;
		this.options = options;
		this.client = client;
		this.complianceMode = options.getSearchOptions().getComplianceMode();
		this.searchMappingsCache = searchMappingsCache;
	}

	/**
	 * Start the provider by creating the REST client and checking the server status.
	 */
	@Override
	public void start() {
		log.debug("Creating elasticsearch provider.");

		ElasticSearchOptions searchOptions = getOptions();

		if (searchOptions.isStartEmbedded()) {
			try {
				processManager.start();
				processManager.startWatchDog();
			} catch (IOException | ZipException e) {
				log.error("Error while starting embedded Elasticsearch server.", e);
				throw new RuntimeException("Error while starting embedded Elasticsearch server", e);
			}
		}

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
	public Completable clear(String indexPattern) {
		Pattern pattern = null;
		if (indexPattern != null) {
			indexPattern = StringUtils.removeStartIgnoreCase(indexPattern, options.getSearchOptions().getPrefix());
			try {
				pattern = Pattern.compile(indexPattern);
			} catch (PatternSyntaxException e) {
				log.warn("Index pattern {} is not valid, synchronizing all indices", e, indexPattern);
			}
		}
		Optional<Pattern> optPattern = Optional.ofNullable(pattern);

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
			}).filter(index -> {
				return optPattern.orElse(IndexHandler.MATCH_ALL).matcher(index).matches();
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

		return Completable.mergeArray(clearIndices, clearPipelines).doFinally(() -> {
			log.info("Sending index clear completed event");
			vertx.get().eventBus().publish(INDEX_CLEAR_FINISHED.address, null);
		});
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
			return client.refresh(installationPrefix() + "*").async()
				.doOnError(error -> {
					log.error("Refreshing of all indices failed.", error);
					throw error(INTERNAL_SERVER_ERROR, "search_error_refresh_failed", error);
				}).ignoreElement()
				.compose(withTimeoutAndLog("Refreshing all indices", true));
		}

		return Observable.fromArray(indices).flatMapCompletable(index -> {
			String fullIndex = installationPrefix() + index;
			return client.refresh(fullIndex).async()
				.doOnError(error -> {
					log.error("Refreshing of indices {" + fullIndex + "} failed.", error);
					throw error(INTERNAL_SERVER_ERROR, "search_error_refresh_failed", error);
				}).ignoreElement()
				.compose(withTimeoutAndLog("Refreshing indices {" + fullIndex + "}", true));
		});
	}

	@Override
	public Single<Set<String>> listIndices() {
		return client.readIndex(installationPrefix() + "*").async().map(json -> {
			return json.fieldNames().stream()
				// Filter again to ensure we only deal with correct names
				.filter(i -> i.startsWith(installationPrefix()))
				// Remove the prefix
				.map(i -> i.substring(installationPrefix().length()))
				.collect(Collectors.toSet());
		});
	}

	@Override
	public Completable createIndex(IndexInfo info) {
		String indexName = installationPrefix() + info.getIndexName();
		return Completable.defer(() -> {

			if (log.isDebugEnabled()) {
				log.debug("Creating ES Index {" + indexName + "}");
			}

			JsonObject json = createIndexSettings(info);
			Completable indexCreation = client.createIndex(indexName, json).async()
				.doOnSuccess(response -> {
					if (log.isDebugEnabled()) {
						log.debug("Create index {" + indexName + "} - {" + info.getSourceInfo() + "} response: {" + response.toString() + "}");
					}
				}).ignoreElement()
				.onErrorResumeNext(error -> isResourceAlreadyExistsError(error) ? Completable.complete() : Completable.error(error));

			return indexCreation;
		}).compose(withTimeoutAndLog("Creating index {" + indexName + "} for {" + info.getSourceInfo() + "}", true));
	}

	@Override
	public Single<JsonObject> getDocument(String index, String uuid) {
		String fullIndex = installationPrefix() + index;

		return client.getDocument(fullIndex, getType(), uuid).async()
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
		return client.deleteDocument(fullIndex, getType(), uuid).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Deleted object {" + uuid + "} from index {" + fullIndex + "}");
				}
			}).ignoreElement()
			.onErrorResumeNext(ignore404)
			.compose(withTimeoutAndLog("Deleting document {" + fullIndex + "} / {" + uuid + "}", true));
	}

	@Override
	public Completable updateDocument(String index, String uuid, JsonObject document, boolean ignoreMissingDocumentError) {
		String fullIndex = installationPrefix() + index;
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Updating object {" + uuid + "} to index.");
		}

		return client.updateDocument(fullIndex, getType(), uuid, new JsonObject().put("doc", document)).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug(
						"Update object {" + uuid + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
			}).ignoreElement().onErrorResumeNext(error -> {
				if (ignoreMissingDocumentError && isNotFoundError(error)) {
					return Completable.complete();
				}
				return Completable.error(error);
			}).compose(withTimeoutAndLog("Updating document {" + fullIndex + "} / {" + uuid + "}", true));
	}

	@Override
	public Completable processBulk(String actions) {
		long start = System.currentTimeMillis();
		return client.processBulk(actions).async()
			.flatMap(response -> {
				boolean errors = response.getBoolean("errors");
				if (errors) {
					log.trace("Error after processing bulk:\n{}", response);
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
					return Single.error(new ElasticsearchBulkResponseError(response));
				}

				if (log.isDebugEnabled()) {
					log.debug("Finished bulk request. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
				return Single.just(response);
			}).ignoreElement()
			.compose(withTimeoutAndLog("Storing document batch.", false));
	}

	@Override
	public Completable processBulk(Collection<? extends Bulkable> entries) {
		if (entries.isEmpty()) {
			return Completable.complete();
		}

		return Flowable.fromIterable(entries)
			.flatMapSingle(Bulkable::toBulkActions)
			.flatMapIterable(list -> list)
			.reduce(new StringBuilder(), (builder, str) -> builder.append(str).append("\n"))
			.map(StringBuilder::toString)
			.doOnSuccess(bulkData -> {
				if (log.isTraceEnabled()) {
					log.trace("Using bulk payload:");
					log.trace(bulkData);
				}
			}).flatMapCompletable(this::processBulk);
	}

	@Override
	public Completable processBulkOld(List<? extends BulkEntry> entries) {
		if (entries.isEmpty()) {
			return Completable.complete();
		}

		String bulkData = entries.stream()
			.map(e -> e.toBulkString(installationPrefix()))
			.collect(Collectors.joining("\n")) + "\n";
		if (log.isTraceEnabled()) {
			log.trace("Using bulk payload:");
			log.trace(bulkData);
		}

		return processBulk(bulkData);
	}

	@Override
	public Completable storeDocument(String index, String uuid, JsonObject document) {
		String fullIndex = installationPrefix() + index;
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Adding object {" + uuid + "} to index {" + fullIndex + "}");
		}
		return client.storeDocument(fullIndex, getType(), uuid, document).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Added object {" + uuid + "} to index {" + fullIndex + "}. Duration " + (System.currentTimeMillis()
						- start) + "[ms]");
				}
			}).ignoreElement().compose(withTimeoutAndLog("Storing document {" + fullIndex + "} / {" + uuid + "}", true));
	}

	@Override
	public Completable deleteIndex(boolean failOnMissingIndex, String... indexNames) {
		String[] fullIndexNames = Arrays.stream(indexNames).map(i -> installationPrefix() + i).toArray(String[]::new);
		String indices = String.join(",", fullIndexNames);
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Deleting indices {" + indices + "}");
		}
		Completable deleteIndex = client.deleteIndex(fullIndexNames).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Deleted index {" + indices + "}. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
			}).ignoreElement();

		if (!failOnMissingIndex) {
			deleteIndex = deleteIndex.onErrorResumeNext(ignore404);
		}
		deleteIndex = deleteIndex.compose(withTimeoutAndLog("Deletion of indices " + indices, !failOnMissingIndex));

		return deleteIndex;
	}

	@Override
	public Completable deregisterPipeline(String name) {
		String fullname = installationPrefix() + name;
		return client.deregisterPlugin(fullname).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Deregistered pipeline {" + fullname + "} response: {" + response.toString() + "}");
				}
			}).ignoreElement()
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
			}).ignoreElement()
			.andThen(client.deleteIndexTemplate(templateName).async().ignoreElement())
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
						log.error("Request failed {" + msg + "}", error.toString());
						log.error(error);
					}
				});
			return ignoreError ? t.onErrorComplete() : t;
		};
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

	@Override
	public boolean isActive() {
		return client != null;
	}

	@Override
	public Completable check(IndexInfo info) {
		String indexName = installationPrefix() + info.getIndexName();

		return client.readIndex(indexName).async().flatMapCompletable(response -> {
			JsonObject existing = response.getJsonObject(indexName).getJsonObject("mappings");
			cleanMappings(existing);

			return getExpectedMapping(info).flatMapCompletable(expected -> {
				// make a copy to not change the original expected (which is cached and will be used again)
				expected = expected.copy();
				cleanMappings(expected);

				// merge the existing mapping into the expected mapping, because ES may have added things (dynamic mapping) when documents were stored
				expected.mergeIn(existing, true);
				if (expected.equals(existing)) {
					log.debug(indexName + ": Mapping is ok");
					return Completable.complete();
				} else {
					log.warn(indexName + ": Mapping is NOT OK. Index will be dropped and re-created.");
					if (log.isDebugEnabled()) {
						log.debug(indexName + ": Expected mapping " + expected + ", but found mapping " + existing);
					}
					// trigger resync for the index
					return client.deleteIndex(indexName).async().ignoreElement().andThen(createIndex(info)).andThen(resync(info.getIndexName()));
				}
			});
		}).onErrorResumeNext(error -> {
			if (isNotFoundError(error)) {
				return createIndex(info).andThen(resync(info.getIndexName()));
			} else {
				log.error("Error while checking index " + indexName, error);
				return Completable.complete();
			}
		}).compose(withTimeoutAndLog("Check mappings of index {" + indexName + "}", false));
	}

	/**
	 * Initiate (but do not wait for end of) resync of the given index and complete
	 * @param indexName index name (without the installation prefix)
	 * @return completable
	 */
	protected Completable resync(String indexName) {
		return Completable.fromAction(() -> {
			SyncEventHandler.invokeSync(vertx.get(), indexName);
		});
	}

	/**
	 * Get the expected mapping for the index.
	 * If the expected mapping is not found in the cache, it will be determined like this:
	 * <ol>
	 * <li>A temporary index with the settings and mappings is created</li>
	 * <li>The mappings are read from ES for that temporary index</li>
	 * <li>The temporary index is deleted</li>
	 * </ol>
	 * The reason for this is that ES will not store the index mapping exactly like the mapping was POSTed when creating the index.
	 * @param info index info
	 * @return single mappings as JsonObject
	 */
	protected Single<JsonObject> getExpectedMapping(IndexInfo info) {
		String cacheKey = info.getIndexName();

		JsonObject cachedMappings = searchMappingsCache.get().get(cacheKey);
		if (cachedMappings != null) {
			return Single.just(cachedMappings);
		}

		JsonObject json = createIndexSettings(info);

		String randomName = info.getIndexName() + UUIDUtil.randomUUID();
		String tempIndexName = randomName.toLowerCase();

		return client.createIndex(tempIndexName, json).async()
			.doOnSuccess(response -> {
				if (log.isDebugEnabled()) {
					log.debug("Created temporary index {" + tempIndexName + "} response: {" + response.toString() + "}");
				}
			})
			.doOnError(error -> {
				log.error("Error getting index mapping for index " + cacheKey, error);
			})
			.flatMap(response -> client.readIndex(tempIndexName).async())
			.flatMap(response -> {
				JsonObject mappings = response.getJsonObject(tempIndexName).getJsonObject("mappings");
				searchMappingsCache.get().put(cacheKey, mappings);
				return client.deleteIndex(tempIndexName).async().ignoreElement()
						.andThen(Single.just(mappings));
			});
	}

	/**
	 * Clean mappings, so that they can be compared
	 * <ul>
	 * <li>Remove entries completely that contain the attribute "dynamic": "true", because those mappings are likely to be changed by ES when documents are indexed (e.g. the attribute "type": "object" is removed from the mapping)</li>
	 * </ul>
	 * @param mapping mappings to be cleaned (will be modified)
	 */
	protected void cleanMappings(JsonObject mapping) {
		Set<String> fieldNames = new HashSet<>(mapping.fieldNames());
		for (String field : fieldNames) {
			Object value = mapping.getValue(field);
			if (value instanceof JsonObject) {
				JsonObject object = (JsonObject) value;
				if (Objects.equals(object.getValue("dynamic"), "true")) {
					mapping.remove(field);
				} else {
					cleanMappings(object);
				}
			}
		}
	}

	private String getType() {
		switch (complianceMode) {
		case ES_6:
			return DEFAULT_TYPE;
		case ES_7:
			return null;
		default:
			throw new RuntimeException("Unknown compliance mode {" + complianceMode + "}");
		}
	}
}
