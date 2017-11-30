package com.gentics.mesh.search.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.elasticsearch.client.Requests.refreshRequest;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.ElasticSearchOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.RxHelper;
import rx.Completable;
import rx.Scheduler;
import rx.Single;

/**
 * Elastic search provider class which implements the {@link SearchProvider} interface.
 */
public class ElasticSearchProvider implements SearchProvider {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);

	private RestHighLevelClient client;

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

		client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http"), new HttpHost("localhost", 9201, "http")));

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
		stop();
		try {
			ElasticSearchOptions searchOptions = options.getSearchOptions();
			if (searchOptions.getDirectory() != null) {
				File storageDirectory = new File(searchOptions.getDirectory());
				if (storageDirectory.exists()) {
					FileUtils.deleteDirectory(storageDirectory);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		start();
	}

	@Override
	public void clear() {
		client.delete(new DeleteRequest().index("_all"));
	}

	@Override
	public void stop() {
		if (client != null) {
			try {
				client.close();
			} catch (IOException e) {
				log.error("Error while closing client", e);
			}
		}
	}

	@Override
	public void refreshIndex(String... indices) {
		client.admin().indices().refresh(refreshRequest().indices(indices)).actionGet();
	}

	private RestHighLevelClient getSearchClient() {
		return client;
	}

	@Override
	public Completable createIndex(IndexInfo info) {
		String indexName = info.getIndexName();
		Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());
		return Completable.create(sub -> {
			if (log.isDebugEnabled()) {
				log.debug("Creating ES Index {" + indexName + "}");
			}
			CreateIndexRequestBuilder createIndexRequestBuilder = getSearchClient().admin().indices().prepareCreate(indexName);

			JsonObject json = createIndexSettings(info);
			createIndexRequestBuilder.setSource(json.encodePrettily(), XContentType.JSON);
			createIndexRequestBuilder.execute(new ActionListener<CreateIndexResponse>() {

				@Override
				public void onResponse(CreateIndexResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Create index {" + indexName + "}response: {" + response.toString() + "}");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Exception e) {
					if (e instanceof ResourceAlreadyExistsException) {
						sub.onCompleted();
					} else {
						sub.onError(e);
						log.error("Error while creating index {" + indexName + "}", e);
					}
				}

			});
		}).observeOn(scheduler);
	}

	@Override
	public Single<Map<String, Object>> getDocument(String index, String uuid) {
		return Single.create(sub -> {
			getSearchClient().prepareGet(index, DEFAULT_TYPE, uuid).execute(new ActionListener<GetResponse>() {

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
			getSearchClient().prepareDelete(index, DEFAULT_TYPE, uuid).execute(new ActionListener<DeleteResponse>() {
				@Override
				public void onResponse(DeleteResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Deleted object {" + uuid + "} from index {" + index + "}");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Exception e) {
					if (e instanceof DocumentMissingException) {
						sub.onCompleted();
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
		Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());
		return Completable.create(sub -> {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Updating object {" + uuid + ":" + DEFAULT_TYPE + "} to index.");
			}
			UpdateRequestBuilder builder = getSearchClient().prepareUpdate(index, DEFAULT_TYPE, uuid);
			builder.setDoc(document.toString());
			builder.execute(new ActionListener<UpdateResponse>() {

				@Override
				public void onResponse(UpdateResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Update object {" + uuid + ":" + DEFAULT_TYPE + "} to index. Duration " + (System.currentTimeMillis() - start)
								+ "[ms]");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Exception e) {
					if (ignoreMissingDocumentError && e instanceof DocumentMissingException) {
						sub.onCompleted();
					} else {
						log.error("Updating object {" + uuid + ":" + DEFAULT_TYPE + "} to index failed. Duration "
								+ (System.currentTimeMillis() - start) + "[ms]", e);
						sub.onError(e);
					}
				}
			});
		}).observeOn(scheduler);
	}

	@Override
	public Completable storeDocumentBatch(String index, Map<String, JsonObject> documents) {
		if (documents.isEmpty()) {
			return Completable.complete();
		}
		return Completable.create(sub -> {
			long start = System.currentTimeMillis();

			BulkRequestBuilder bulk = getSearchClient().prepareBulk();

			// Add index requests for each document to the bulk request
			for (Map.Entry<String, JsonObject> entry : documents.entrySet()) {
				String documentId = entry.getKey();
				JsonObject document = entry.getValue();
				IndexRequestBuilder indexRequestBuilder = getSearchClient().prepareIndex(index, DEFAULT_TYPE, documentId);
				indexRequestBuilder.setSource(document.toString());
				bulk.add(indexRequestBuilder);
			}

			bulk.execute(new ActionListener<BulkResponse>() {
				@Override
				public void onResponse(BulkResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Finished bulk  store request on index {" + index + ":" + DEFAULT_TYPE + "}. Duration "
								+ (System.currentTimeMillis() - start) + "[ms]");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Bulk store on index {" + index + ":" + DEFAULT_TYPE + "} to index failed. Duration "
							+ (System.currentTimeMillis() - start) + "[ms]", e);
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
			IndexRequestBuilder builder = getSearchClient().prepareIndex(index, DEFAULT_TYPE, uuid);

			builder.setSource(document.toString());
			builder.execute(new ActionListener<IndexResponse>() {

				@Override
				public void onResponse(IndexResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Added object {" + uuid + ":" + DEFAULT_TYPE + "} to index. Duration " + (System.currentTimeMillis() - start)
								+ "[ms]");
					}
					sub.onCompleted();
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
			getSearchClient().admin().indices().prepareDelete(indexName).execute(new ActionListener<DeleteIndexResponse>() {

				public void onResponse(DeleteIndexResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Deleted index {" + indexName + "}. Duration " + (System.currentTimeMillis() - start) + "[ms]");
					}
					sub.onCompleted();
				};

				@Override
				public void onFailure(Exception e) {
					if (e instanceof IndexNotFoundException && !failOnMissingIndex) {
						sub.onCompleted();
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
		Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());
		return Completable.create(sub -> {
			JsonObject json = createIndexSettings(info);
			if (log.isDebugEnabled()) {
				log.debug("Validating index configuration {" + json.encodePrettily() + "}");
			}

			String randomName = info.getIndexName() + UUIDUtil.randomUUID();
			String templateName = randomName.toLowerCase();
			json.put("template", templateName);
			PutIndexTemplateRequestBuilder builder = getSearchClient().admin().indices().preparePutTemplate(templateName)
					.setSource(json.encodePrettily().getBytes(), XContentType.JSON);

			builder.execute(new ActionListener<PutIndexTemplateResponse>() {
				@Override
				public void onResponse(PutIndexTemplateResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Created template {" + templateName + "} response: {" + response.toString() + "}");
					}

					Mesh.vertx().executeBlocking(bh -> {
						getSearchClient().admin().indices().prepareDeleteTemplate(templateName)
								.execute(new ActionListener<DeleteIndexTemplateResponse>() {

									@Override
									public void onResponse(DeleteIndexTemplateResponse response) {
										sub.onCompleted();
									}

									@Override
									public void onFailure(Exception e) {
										sub.onError(e);
									}

								});
					}, false, rh -> {

					});
				}

				@Override
				public void onFailure(Exception e) {
					sub.onError(error(BAD_REQUEST, "schema_error_index_validation", e.getMessage()));
				}
			});
		}).observeOn(scheduler);
	}

	@Override
	public String getVendorName() {
		return "elasticsearch";
	}

	@Override
	public String getVersion() {
		NodesInfoResponse info = client.admin().cluster().prepareNodesInfo().all().get();
		return info.getNodes().get(0).getVersion().toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getClient() {
		return (T) client;
	}

}
