package com.gentics.mesh.search.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.rest.RestStatus;

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

	private RestHighLevelClient client;

	private MeshOptions options;

	// private Scheduler scheduler;

	// private WorkerExecutor workerPool;

	private ElasticsearchProcessManager processManager = new ElasticsearchProcessManager(Mesh.mesh().getVertx());

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

		// workerPool = Mesh.vertx().createSharedWorkerExecutor("searchWorker", 15, 10 * 1000 * 1000);
		// scheduler = RxHelper.blockingScheduler(workerPool);

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

		RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[hosts.size()]));
		client = new RestHighLevelClient(builder);

		if (waitForCluster) {
			waitForCluster(client, 45);
			if (log.isDebugEnabled()) {
				log.debug("Waited for elasticsearch shard: " + (System.currentTimeMillis() - start) + "[ms]");
			}
		}
	}

	private void waitForCluster(RestHighLevelClient client, long timeoutInSec) {
		long start = System.currentTimeMillis();
		// Wait until the cluster is ready
		while (true) {
			if ((System.currentTimeMillis() - start) / 1000 > timeoutInSec) {
				log.debug("Timeout of {" + timeoutInSec + "} reached.");
				break;
			}
			try {
				log.debug("Checking elasticsearch status...");
				MainResponse response = client.info();
				boolean ready = response.isAvailable();
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

		Maybe<List<String>> indexInfo = Maybe.create(sub -> {
			// Read all indices and locate indices which have been created for/by mesh.
			client.getLowLevelClient().performRequestAsync(GET.toString(), "/_all", new ResponseListener() {

				@Override
				public void onSuccess(Response response) {
					List<String> indices = Collections.emptyList();
					try (InputStream ins = response.getEntity().getContent()) {
						String json = IOUtils.toString(ins);
						JsonObject indexInfo = new JsonObject(json);
						indices = indexInfo.fieldNames().stream().filter(e -> e.startsWith(INDEX_PREFIX)).collect(Collectors.toList());
					} catch (UnsupportedOperationException | IOException e1) {
						sub.onError(e1);
						return;
					}
					if (indices.isEmpty()) {
						sub.onComplete();
					} else {
						sub.onSuccess(indices);
					}
				}

				@Override
				public void onFailure(Exception exception) {
					sub.onError(exception);
				}
			});
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

		// if (scheduler != null) {
		// log.info("Shutting down Elasticsearch job scheduler.");
		// scheduler.shutdown();
		// }
		// if (workerPool != null) {
		// log.info("Closing Elasticsearch worker pool.");
		// workerPool.close();
		// }
	}

	@Override
	public Completable refreshIndex(String... indices) {
		String indicesStr = StringUtils.join(indices, ",");
		return Completable.create(sub -> {
			String path = "/_refresh";
			if (indices.length > 0) {
				path = "/" + indicesStr + "/_refresh";
			}
			if (log.isDebugEnabled()) {
				log.debug("Refreshing indices with path {" + path + "}");
			}
			client.getLowLevelClient().performRequestAsync(POST.toString(), path, new ResponseListener() {

				@Override
				public void onSuccess(Response response) {
					sub.onComplete();
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Refreshing of indices {" + indicesStr + "} failed.", e);
					sub.onError(error(INTERNAL_SERVER_ERROR, "search_error_refresh_failed", e));
				}
			});
		}).compose(withTimeoutAndLog("Refreshing indices {" + indicesStr + "}"));
	}

	@Override
	public Completable createIndex(IndexInfo info) {
		String indexName = info.getIndexName();

		return Completable.create(sub -> {
			if (log.isDebugEnabled()) {
				log.debug("Creating ES Index {" + indexName + "}");
			}

			JsonObject json = createIndexSettings(info);
			HttpEntity entity = new NStringEntity(json.toString(), ContentType.APPLICATION_JSON);

			// TODO replace this code with high level client when upgrading to 6.2.0
			getSearchClient().getLowLevelClient().performRequestAsync(PUT.toString(), indexName, Collections.emptyMap(), entity,
					new ResponseListener() {

						@Override
						public void onSuccess(Response response) {
							if (log.isDebugEnabled()) {
								log.debug("Create index {" + indexName + "}response: {" + response.toString() + "}");
							}
							sub.onComplete();
						}

						@Override
						public void onFailure(Exception e) {
							if (e instanceof ResponseException) {
								ResponseException re = (ResponseException) e;
								try (InputStream ins = re.getResponse().getEntity().getContent()) {
									String json = IOUtils.toString(ins);
									JsonObject ob = new JsonObject(json);
									String type = ob.getJsonObject("error").getString("type");
									if (type.equals("resource_already_exists_exception")) {
										sub.onComplete();
									} else {
										if (log.isDebugEnabled()) {
											log.debug("Got failure response {" + ob.encodePrettily() + "}");
										}
										sub.onError(e);
									}
								} catch (UnsupportedOperationException | IOException e1) {
									sub.onError(e1);
								}
							} else {
								log.error("Error while creating index {" + indexName + "}", e);
								sub.onError(e);
							}
						}

					});
		}).compose(withTimeoutAndLog("Creating index {" + indexName + "}"));
	}

	@Override
	public Single<Map<String, Object>> getDocument(String index, String uuid) {
		Single<Map<String, Object>> single = Single.create(sub -> {
			getSearchClient().getAsync(new GetRequest(index, DEFAULT_TYPE, uuid), new ActionListener<GetResponse>() {

				@Override
				public void onResponse(GetResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Get object {" + uuid + "} from index {" + index + "}");
					}
					sub.onSuccess(response.getSourceAsMap());
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Could not get object {" + uuid + "} from index {" + index + "}");
					sub.onError(e);
				}
			});
		});
		return single.timeout(getOptions().getTimeout(), TimeUnit.MILLISECONDS).doOnError(log::error);
	}

	@Override
	public Completable deleteDocument(String index, String uuid) {
		return Completable.create(sub -> {
			if (log.isDebugEnabled()) {
				log.debug("Deleting document {" + uuid + "} from index {" + index + "}.");
			}
			getSearchClient().deleteAsync(new DeleteRequest(index, DEFAULT_TYPE, uuid), new ActionListener<DeleteResponse>() {
				@Override
				public void onResponse(DeleteResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Deleted object {" + uuid + "} from index {" + index + "}");
					}
					sub.onComplete();
				}

				@Override
				public void onFailure(Exception e) {
					if (e instanceof DocumentMissingException) {
						sub.onComplete();
					} else {
						log.error("Could not delete object {" + uuid + "} from index {" + index + "}");
						sub.onError(e);
					}
				}
			});
		}).compose(withTimeoutAndLog("Deleting document {" + index + "} / {" + uuid + "}"));
	}

	@Override
	public Completable updateDocument(String index, String uuid, JsonObject document, boolean ignoreMissingDocumentError) {
		return Completable.create(sub -> {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Updating object {" + uuid + ":" + DEFAULT_TYPE + "} to index.");
			}

			UpdateRequest request = new UpdateRequest(index, DEFAULT_TYPE, uuid);
			request.doc(document.toString(), XContentType.JSON);
			getSearchClient().updateAsync(request, new ActionListener<UpdateResponse>() {

				@Override
				public void onResponse(UpdateResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Update object {" + uuid + ":" + DEFAULT_TYPE + "} to index. Duration " + (System.currentTimeMillis() - start)
								+ "[ms]");
					}
					sub.onComplete();
				}

				@Override
				public void onFailure(Exception e) {
					if (e instanceof ElasticsearchStatusException) {
						ElasticsearchStatusException se = (ElasticsearchStatusException) e;
						if (ignoreMissingDocumentError && RestStatus.NOT_FOUND.equals(se.status())) {
							sub.onComplete();
							return;
						}
					}
					log.error("Updating object {" + uuid + ":" + DEFAULT_TYPE + "} to index failed. Duration " + (System.currentTimeMillis() - start)
							+ "[ms]", e);
					sub.onError(e);
				}
			});
		}).compose(withTimeoutAndLog("Updating document {" + index + "} / {" + uuid + "}"));
	}

	@Override
	public Completable storeDocumentBatch(String index, Map<String, JsonObject> documents) {
		if (documents.isEmpty()) {
			return Completable.complete();
		}
		return Completable.create(sub -> {
			long start = System.currentTimeMillis();

			BulkRequest request = new BulkRequest();

			// Add index requests for each document to the bulk request
			for (Map.Entry<String, JsonObject> entry : documents.entrySet()) {
				String documentId = entry.getKey();
				JsonObject document = entry.getValue();
				request.add(new IndexRequest(index, DEFAULT_TYPE, documentId).source(document.toString(), XContentType.JSON));
			}

			getSearchClient().bulkAsync(request, new ActionListener<BulkResponse>() {
				@Override
				public void onResponse(BulkResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Finished bulk  store request on index {" + index + ":" + DEFAULT_TYPE + "}. Duration "
								+ (System.currentTimeMillis() - start) + "[ms]");
					}
					sub.onComplete();
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Bulk store on index {" + index + ":" + DEFAULT_TYPE + "} to index failed. Duration "
							+ (System.currentTimeMillis() - start) + "[ms]", e);
					sub.onError(e);
				}

			});
		}).compose(withTimeoutAndLog("Storing document batch"));
	}

	@Override
	public Completable storeDocument(String index, String uuid, JsonObject document) {
		return Completable.create(sub -> {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Adding object {" + uuid + ":" + DEFAULT_TYPE + "} to index {" + index + "}");
			}
			IndexRequest indexRequest = new IndexRequest(index, DEFAULT_TYPE, uuid);
			indexRequest.source(document.toString(), XContentType.JSON);
			getSearchClient().indexAsync(indexRequest, new ActionListener<IndexResponse>() {

				@Override
				public void onResponse(IndexResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Added object {" + uuid + ":" + DEFAULT_TYPE + "} to index {" + index + "}. Duration "
								+ (System.currentTimeMillis() - start) + "[ms]");
					}
					sub.onComplete();
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Adding object {" + uuid + ":" + DEFAULT_TYPE + "} to index {" + index + "} failed. Duration "
							+ (System.currentTimeMillis() - start) + "[ms]", e);
					sub.onError(e);
				}
			});
		}).compose(withTimeoutAndLog("Storing document {" + index + "} / {" + uuid + "}"));
	}

	@Override
	public Completable deleteIndex(boolean failOnMissingIndex, String... indexNames) {
		return Completable.create(sub -> {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Deleting index {" + indexNames + "}");
			}
			DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexNames);
			getSearchClient().indices().deleteIndexAsync(deleteIndexRequest, new ActionListener<DeleteIndexResponse>() {

				public void onResponse(DeleteIndexResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Deleted index {" + indexNames + "}. Duration " + (System.currentTimeMillis() - start) + "[ms]");
					}
					sub.onComplete();
				};

				@Override
				public void onFailure(Exception e) {
					if (e instanceof IndexNotFoundException && !failOnMissingIndex) {
						sub.onComplete();
					} else {
						log.error("Deleting index {" + indexNames + "} failed. Duration " + (System.currentTimeMillis() - start) + "[ms]", e);
						sub.onError(e);
					}
				}
			});
		}).compose(withTimeoutAndLog("Deletion of index " + indexNames));
	}

	@Override
	public Completable validateCreateViaTemplate(IndexInfo info) {
		return Completable.create(sub -> {
			JsonObject json = createIndexSettings(info);
			if (log.isDebugEnabled()) {
				log.debug("Validating index configuration {" + json.encodePrettily() + "}");
			}

			String randomName = info.getIndexName() + UUIDUtil.randomUUID();
			String templateName = randomName.toLowerCase();
			json.put("template", templateName);

			HttpEntity entity = new NStringEntity(json.toString(), ContentType.APPLICATION_JSON);
			getSearchClient().getLowLevelClient().performRequestAsync(PUT.toString(), "_template/" + templateName, Collections.emptyMap(), entity,
					new ResponseListener() {
						@Override
						public void onSuccess(Response response) {
							if (log.isDebugEnabled()) {
								log.debug("Created template {" + templateName + "} response: {" + response.toString() + "}");
							}

							getSearchClient().getLowLevelClient().performRequestAsync(DELETE.toString(), "_template/" + templateName,
									new ResponseListener() {

										@Override
										public void onSuccess(Response response) {
											sub.onComplete();
										}

										@Override
										public void onFailure(Exception e) {
											sub.onError(e);
										}

									});
						}

						@Override
						public void onFailure(Exception e) {
							if (e instanceof ResponseException) {
								ResponseException re = (ResponseException) e;
								try (InputStream ins = re.getResponse().getEntity().getContent()) {
									String json = IOUtils.toString(ins);
									JsonObject errorInfo = new JsonObject(json);
									sub.onError(error(BAD_REQUEST, "schema_error_index_validation",
											errorInfo.getJsonObject("error").getString("reason")));
								} catch (UnsupportedOperationException | IOException e1) {
									sub.onError(error(BAD_REQUEST, "schema_error_index_validation", e1.getMessage()));
								}

							} else {
								sub.onError(error(BAD_REQUEST, "schema_error_index_validation", e.getMessage()));
							}
						}
					});
		}).compose(withTimeoutAndLog("Template validation"));
	}

	@Override
	public String getVendorName() {
		return "elasticsearch";
	}

	@Override
	public String getVersion() {
		try {
			MainResponse info = client.info();
			return info.getVersion().toString();
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

	private RestHighLevelClient getSearchClient() {
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

	// private CompletableSource withTimeoutAndLog(Completable c) {
	// Long timeout = getOptions().getTimeout();
	// return c.timeout(timeout, TimeUnit.MILLISECONDS).doOnError(error -> {
	// if (error instanceof TimeoutException) {
	// log.error("The operation failed since the timeout of {" + timeout + "} ms has been reached.");
	// } else {
	// log.error(error);
	// }
	// }).onErrorComplete();
	// }

}
