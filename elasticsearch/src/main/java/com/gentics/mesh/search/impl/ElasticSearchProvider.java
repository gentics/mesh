package com.gentics.mesh.search.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.elasticsearch.client.Requests.refreshRequest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryAction;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin;
import org.elasticsearch.plugin.discovery.multicast.MulticastDiscoveryPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.SearchHit;

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

	private Client client;

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
		trigramsAnalyzer.put("tokenizer", "my_ngram_tokenizer");
		trigramsAnalyzer.put("filter", new JsonArray().add("lowercase"));

		JsonObject analysis = new JsonObject();
		analysis.put("analyzer", new JsonObject().put("trigrams", trigramsAnalyzer));
		analysis.put("tokenizer", new JsonObject().put("my_ngram_tokenizer", tokenizer));
		return new JsonObject().put("analysis", analysis);

	}

	@Override
	public void start() {
		if (log.isDebugEnabled()) {
			log.debug("Creating elasticsearch node");
		}

		ElasticSearchOptions searchOptions = options.getSearchOptions();
		long start = System.currentTimeMillis();
		Builder builder = Settings.settingsBuilder()

				.put("threadpool.index.queue_size", -1)

				.put("http.enabled", searchOptions.isHttpEnabled())

				.put("http.cors.enabled", "true").put("http.cors.allow-origin", "*")

				.put("path.home", searchOptions.getDirectory())

				.put("node.name", options.getNodeName())

				.put("transport.tcp.port", searchOptions.getTransportPort())

				.put("plugin.types", DeleteByQueryPlugin.class.getName())

				// .put("index.store.type", "mmapfs")

				.put("index.max_result_window", Integer.MAX_VALUE);

		builder.put("node.meshVersion", Mesh.getPlainVersion());
		ClusterOptions clusterOptions = options.getClusterOptions();
		if (clusterOptions.isEnabled()) {
			// We append the mesh version to the cluster name to ensure that no clusters from different mesh versions can be formed.
			builder.put("cluster.name", clusterOptions.getClusterName() + "-" + Mesh.getPlainVersion());
			// We run a multi-master environment. Every node should be able to be elected as master
			builder.put("node.master", true);
			builder.put("network.host", clusterOptions.getNetworkHost());
			builder.put("discovery.zen.ping.multicast.enabled", true);
			// TODO configure public and bind host
		} else {
			// TODO use transport.type: local for ES5
			builder.put("node.local", true);
		}

		// Add custom properties
		for (Entry<String, Object> entry : searchOptions.getParameters().entrySet()) {
			builder.put(entry.getKey(), entry.getValue());
		}
		Settings settings = builder.build();

		Set<Class<? extends Plugin>> classpathPlugins = new HashSet<>();
		classpathPlugins.add(DeleteByQueryPlugin.class);
		if (clusterOptions.isEnabled()) {
			classpathPlugins.add(MulticastDiscoveryPlugin.class);
		}
		node = new MeshNode(settings, classpathPlugins);
		node.start();
		client = node.client();
		if (log.isDebugEnabled()) {
			log.debug("Waited for elasticsearch shard: " + (System.currentTimeMillis() - start) + "[ms]");
		}

		// builder.put("node.master", false);
		// builder.put("node.data", false);
		// try {
		// node = NodeBuilder.nodeBuilder().settings(builder.build()).clusterName("elasticsearch").local(true).node();
		// client = TransportClient.builder().build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("127.0.0.1"), 9300));
		// } catch (UnknownHostException e) {
		// e.printStackTrace();
		// }

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
		client.admin().indices().prepareDelete("_all").execute().actionGet();
	}

	@Override
	public void stop() {
		if (client != null) {
			client.close();
		}
		if (node != null) {
			node.close();
		}
	}

	@Override
	public void refreshIndex(String... indices) {
		client.admin().indices().refresh(refreshRequest().indices(indices)).actionGet();
	}

	private Client getSearchClient() {
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
			createIndexRequestBuilder.setSource(json.encodePrettily());
			createIndexRequestBuilder.execute(new ActionListener<CreateIndexResponse>() {

				@Override
				public void onResponse(CreateIndexResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Create index {" + indexName + "}response: {" + response.toString() + "}");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Throwable e) {
					if (e instanceof IndexAlreadyExistsException) {
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
			getSearchClient().prepareGet(index, DEFAULT_TYPE, uuid).execute().addListener(new ActionListener<GetResponse>() {

				@Override
				public void onResponse(GetResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Get object {" + uuid + "} from index {" + index + "}");
					}
					sub.onSuccess(response.getSourceAsMap());
				}

				@Override
				public void onFailure(Throwable e) {
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
			getSearchClient().prepareDelete(index, DEFAULT_TYPE, uuid).execute().addListener(new ActionListener<DeleteResponse>() {
				@Override
				public void onResponse(DeleteResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Deleted object {" + uuid + "} from index {" + index + "}");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Throwable e) {
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
			builder.execute().addListener(new ActionListener<UpdateResponse>() {

				@Override
				public void onResponse(UpdateResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Update object {" + uuid + ":" + DEFAULT_TYPE + "} to index. Duration " + (System.currentTimeMillis() - start)
								+ "[ms]");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Throwable e) {
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

			bulk.execute().addListener(new ActionListener<BulkResponse>() {
				@Override
				public void onResponse(BulkResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Finished bulk  store request on index {" + index + ":" + DEFAULT_TYPE + "}. Duration "
								+ (System.currentTimeMillis() - start) + "[ms]");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Throwable e) {
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
			builder.execute().addListener(new ActionListener<IndexResponse>() {

				@Override
				public void onResponse(IndexResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Added object {" + uuid + ":" + DEFAULT_TYPE + "} to index. Duration " + (System.currentTimeMillis() - start)
								+ "[ms]");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Throwable e) {
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
				public void onFailure(Throwable e) {
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
	public Completable clearIndex(String indexName) {
		return Completable.create(sub -> {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Clearing index {" + indexName + "}");
			}

			DeleteByQueryRequestBuilder builder = new DeleteByQueryRequestBuilder(getSearchClient(), DeleteByQueryAction.INSTANCE);
			builder.setIndices(indexName).setQuery(QueryBuilders.matchAllQuery()).execute().addListener(new ActionListener<DeleteByQueryResponse>() {
				public void onResponse(DeleteByQueryResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Clearing index {" + indexName + "}. Duration " + (System.currentTimeMillis() - start) + "[ms]");
					}
					sub.onCompleted();
				};

				@Override
				public void onFailure(Throwable e) {
					if (e instanceof IndexNotFoundException) {
						if (log.isDebugEnabled()) {
							log.debug("Clearing index failed since the index does not exists. We ignore this error", e);
						}
						sub.onCompleted();
					} else {
						log.error("Clearing index {" + indexName + "} failed. Duration " + (System.currentTimeMillis() - start) + "[ms]", e);
						sub.onError(e);
					}
				}
			});

		});
	}

	@Override
	public Single<Integer> deleteDocumentsViaQuery(String searchQuery, String... indices) {
		return Single.create(sub -> {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Deleting documents from indices {" + Arrays.toString(indices) + "} via query {" + searchQuery + "}");
			}
			SearchRequestBuilder builder = client.prepareSearch(indices).setSource(searchQuery);

			Set<Completable> obs = new HashSet<>();
			builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			builder.execute().addListener(new ActionListener<SearchResponse>() {
				@Override
				public void onResponse(SearchResponse response) {

					if (log.isDebugEnabled()) {
						log.debug("Found {" + response.getHits().totalHits() + "} which match the deletion query.");
					}
					// Invoke the deletion for each found document
					for (SearchHit hit : response.getHits()) {
						obs.add(deleteDocument(hit.getIndex(), hit.getId()));
					}
					Completable.merge(obs).await();

					if (log.isDebugEnabled()) {
						log.debug("Deleted {" + obs.size() + "} documents from indices {" + Arrays.toString(indices) + "}");
					}
					sub.onSuccess(obs.size());
				}

				@Override
				public void onFailure(Throwable e) {
					log.error("Error deleting from indices {" + Arrays.toString(indices) + "}. Duration " + (System.currentTimeMillis() - start)
							+ "[ms]", e);
					sub.onError(e);
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
					.setSource(json.encodePrettily());

			builder.execute(new ActionListener<PutIndexTemplateResponse>() {
				@Override
				public void onResponse(PutIndexTemplateResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Created template {" + templateName + "} response: {" + response.toString() + "}");
					}
					getSearchClient().admin().indices().prepareDeleteTemplate(templateName).get();
					sub.onCompleted();
				}

				@Override
				public void onFailure(Throwable e) {
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
		return info.getAt(0).getVersion().number();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getClient() {
		return (T) client;
	}

}
