package com.gentics.mesh.search.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.PUT;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
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
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.node.Node;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchHost;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Elastic search provider class which implements the {@link SearchProvider} interface.
 */
public class ElasticSearchProvider implements SearchProvider {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);

	private RestHighLevelClient client;

	private Node node;

	private MeshOptions options;

	public ElasticSearchProvider() {
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
	public void start() {
		if (log.isDebugEnabled()) {
			log.debug("Creating elasticsearch node");
		}

		ElasticSearchOptions searchOptions = options.getSearchOptions();
		long start = System.currentTimeMillis();

		List<HttpHost> hosts = searchOptions.getHosts().stream().map(hostConfig -> {
			return new HttpHost(hostConfig.getHostname(), hostConfig.getPort(), hostConfig.getProtocol());
		}).collect(Collectors.toList());

		RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[hosts.size()]));
		client = new RestHighLevelClient(builder);

		// waitForCluster(client, 45);
		if (log.isDebugEnabled()) {
			log.debug("Waited for elasticsearch shard: " + (System.currentTimeMillis() - start) + "[ms]");
		}

	}

	private void waitForCluster(Client client, long timeoutInSec) {
		long start = System.currentTimeMillis();
		// Wait until the cluster is ready
		while (true) {
			if ((System.currentTimeMillis() - start) / 1000 > timeoutInSec) {
				log.debug("Timeout of {" + timeoutInSec + "} reached.");
				break;
			}
			log.debug("Checking elasticsearch status...");
			ClusterHealthResponse response = client.admin().cluster().prepareHealth().get(TimeValue.timeValueSeconds(10));
			log.debug("Elasticsearch status is: " + response.getStatus());
			if (response.getStatus() != ClusterHealthStatus.RED) {
				log.info("Elasticsearch status {" + response.getStatus() + "}. Releasing lock after " + (System.currentTimeMillis() - start) + " ms");
				return;
			}
			try {
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

	@Override
	public void clear() {
		// TODO only delete indices of mesh. Don't touch other indices!
		// client.delete(new DeleteRequest().index("_all"));
	}

	@Override
	public void stop() throws IOException {
		if (client != null) {
			client.close();
		}
		if (node != null) {
			node.close();
		}
	}

	@Override
	public void refreshIndex(String... indices) {
		// String indicesStr = StringUtils.join(indices, ",");
		// String path = "/_refresh";
		// if (indices.length > 0) {
		// path = "/" + indicesStr + "/_refresh";
		// }
		// try {
		// if (log.isDebugEnabled()) {
		// log.debug("Refreshing indices with path {" + path + "}");
		// }
		// client.getLowLevelClient().performRequest(POST.toString(), path);
		// } catch (IOException e) {
		// log.error("Refreshing of indices {" + indicesStr + "} failed.", e);
		// throw error(INTERNAL_SERVER_ERROR, "Refreshing indices failed.");
		// }
	}

	private RestHighLevelClient getSearchClient() {
		return client;
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
									if (log.isDebugEnabled()) {
										log.debug("Got failure response {" + ob.encodePrettily() + "}");
									}
									String type = ob.getJsonObject("error").getString("type");
									if (type.equals("resource_already_exists_exception")) {
										sub.onComplete();
									} else {
										sub.onError(e);
									}
								} catch (UnsupportedOperationException | IOException e1) {
									// TODO Auto-generated catch block
									sub.onError(e1);
								}
							} else {
								sub.onError(e);
								log.error("Error while creating index {" + indexName + "}", e);
							}
						}

					});
		});
	}

	@Override
	public Single<Map<String, Object>> getDocument(String index, String uuid) {
		return Single.create(sub -> {
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
		});
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
					if (ignoreMissingDocumentError && e instanceof DocumentMissingException) {
						sub.onComplete();
					} else {
						log.error("Updating object {" + uuid + ":" + DEFAULT_TYPE + "} to index failed. Duration " + (System.currentTimeMillis()
								- start) + "[ms]", e);
						sub.onError(e);
					}
				}
			});
		});
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
						log.debug("Finished bulk  store request on index {" + index + ":" + DEFAULT_TYPE + "}. Duration " + (System
								.currentTimeMillis() - start) + "[ms]");
					}
					sub.onComplete();
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Bulk store on index {" + index + ":" + DEFAULT_TYPE + "} to index failed. Duration " + (System.currentTimeMillis()
							- start) + "[ms]", e);
					sub.onError(e);
				}

			});
		});
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
						log.debug("Added object {" + uuid + ":" + DEFAULT_TYPE + "} to index. Duration " + (System.currentTimeMillis() - start)
								+ "[ms]");
					}
					sub.onComplete();
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Adding object {" + uuid + ":" + DEFAULT_TYPE + "} to index failed. Duration " + (System.currentTimeMillis() - start)
							+ "[ms]", e);
					sub.onError(e);
				}
			});
		});
	}

	@Override
	public Completable deleteIndex(String indexName, boolean failOnMissingIndex) {
		return Completable.create(sub -> {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Deleting index {" + indexName + "}");
			}
			DeleteRequest deleteRequest = new DeleteRequest(indexName);
			getSearchClient().deleteAsync(deleteRequest, new ActionListener<DeleteResponse>() {

				public void onResponse(DeleteResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Deleted index {" + indexName + "}. Duration " + (System.currentTimeMillis() - start) + "[ms]");
					}
					sub.onComplete();
				};

				@Override
				public void onFailure(Exception e) {
					if (e instanceof IndexNotFoundException && !failOnMissingIndex) {
						sub.onComplete();
					} else {
						log.error("Deleting index {" + indexName + "} failed. Duration " + (System.currentTimeMillis() - start) + "[ms]", e);
						sub.onError(e);
					}
				}
			});
		});
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
							sub.onError(error(BAD_REQUEST, "schema_error_index_validation", e.getMessage()));
						}
					});
		});
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

}
