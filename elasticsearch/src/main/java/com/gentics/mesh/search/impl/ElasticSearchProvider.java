package com.gentics.mesh.search.impl;

import static org.elasticsearch.client.Requests.refreshRequest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
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
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.SearchHit;

import com.gentics.mesh.cli.MeshNameProvider;
import com.gentics.mesh.etc.config.ElasticSearchOptions;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import rx.Completable;
import rx.Single;

/**
 * Elastic search provider class which implements the {@link SearchProvider} interface.
 */
public class ElasticSearchProvider implements SearchProvider {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);

	private Node node;

	private ElasticSearchOptions options;

	public static final String DEFAULT_INDEX_SETTINGS_FILENAME = "default-es-index-settings.json";

	public static String DEFAULT_INDEX_SETTINGS;
	static {
		try {
			DEFAULT_INDEX_SETTINGS = IOUtils.toString(ElasticSearchProvider.class.getResourceAsStream("/" + DEFAULT_INDEX_SETTINGS_FILENAME));
		} catch (IOException e) {
			throw new RuntimeException("Could not load default index settings", e);
		}
	}

	@Override
	public void start() {
		if (log.isDebugEnabled()) {
			log.debug("Creating elasticsearch node");
		}
		long start = System.currentTimeMillis();
		Settings settings = Settings.builder()

				.put("threadpool.index.queue_size", -1)

				.put("http.enabled", options.isHttpEnabled())

				.put("http.cors.enabled", "true").put("http.cors.allow-origin", "*")

				.put("path.home", options.getDirectory())

				.put("node.name", MeshNameProvider.getInstance().getName())

				.put("node.local", true)

				// .put("index.store.type", "mmapfs")

				.put("index.max_result_window", Integer.MAX_VALUE)

				.build();

		Set<Class<? extends Plugin>> classpathPlugins = new HashSet<>();
		// TODO configure ES cluster options
		node = new Node(settings);
		try {
			node.start();
		} catch (NodeValidationException e) {
			throw new RuntimeException("Could not start search node", e);
		}
		if (log.isDebugEnabled()) {
			log.debug("Waited for elasticsearch shard: " + (System.currentTimeMillis() - start) + "[ms]");
		}
	}

	/**
	 * Initialise and start the search provider using the given options.
	 * 
	 * @param options
	 * @return Fluent API
	 */
	public ElasticSearchProvider init(ElasticSearchOptions options) {
		this.options = options;
		start();
		return this;
	}

	@Override
	public Node getNode() {
		return node;
	}

	@Override
	public void reset() {
		if (log.isDebugEnabled()) {
			log.debug("Resetting elastic search");
		}
		stop();
		try {
			if (options.getDirectory() != null) {
				File storageDirectory = new File(options.getDirectory());
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
		node.client().admin().indices().prepareDelete("_all").execute().actionGet();
	}

	@Override
	public void stop() {
		try {
			node.close();
		} catch (IOException e) {
			throw new RuntimeException("Could not close ElasticSearch", e);
		}
	}

	@Override
	public void refreshIndex(String... indices) {
		getNode().client().admin().indices().refresh(refreshRequest().indices(indices)).actionGet();
	}

	private Client getSearchClient() {
		return getNode().client();
	}

	@Override
	public Completable createIndex(String indexName) {
		// TODO Add method which will be used to create an index and set a custom mapping
		return Completable.create(sub -> {
			if (log.isDebugEnabled()) {
				log.debug("Creating ES Index {" + indexName + "}");
			}
			CreateIndexRequestBuilder createIndexRequestBuilder = getSearchClient().admin().indices().prepareCreate(indexName);

			createIndexRequestBuilder.setSettings(createDefaultIndexSettings().toString(), XContentType.JSON);
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
					sub.onError(e);
					log.error("Error while creating index {" + indexName + "}", e);
				}
			});
		});
	}

	/**
	 * Create the default index settings.
	 * 
	 * @return
	 */
	private JsonObject createDefaultIndexSettings() {
		JsonObject settings = new JsonObject(DEFAULT_INDEX_SETTINGS);
		if (log.isDebugEnabled()) {
			log.debug("Using index settings: ");
			log.debug(settings.encodePrettily());
		}
		return settings;
	}

	@Override
	public Single<Map<String, Object>> getDocument(String index, String type, String uuid) {
		return Single.create(sub -> {
			getSearchClient().prepareGet(index, type, uuid).execute(new ActionListener<GetResponse>() {

				@Override
				public void onResponse(GetResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Get object {" + uuid + ":" + type + "} from index {" + index + "}");
					}
					sub.onSuccess(response.getSourceAsMap());
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Could not get object {" + uuid + ":" + type + "} from index {" + index + "}");
					sub.onError(e);
				}
			});
		});
	}

	@Override
	public Completable deleteDocument(String index, String type, String uuid) {
		return Completable.create(sub -> {
			if (log.isDebugEnabled()) {
				log.debug("Deleting document {" + uuid + ":" + type + "} from index {" + index + "}.");
			}
			getSearchClient().prepareDelete(index, type, uuid).execute(new ActionListener<DeleteResponse>() {
				@Override
				public void onResponse(DeleteResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Deleted object {" + uuid + ":" + type + "} from index {" + index + "}");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Could not delete object {" + uuid + ":" + type + "} from index {" + index + "}");
					sub.onError(e);
				}
			});
		});
	}

	@Override
	public Completable updateDocument(String index, String type, String uuid, JsonObject document) {
		return Completable.create(sub -> {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Updating object {" + uuid + ":" + type + "} to index.");
			}
			UpdateRequestBuilder builder = getSearchClient().prepareUpdate(index, type, uuid);
			builder.setDoc(document.toString());
			builder.execute(new ActionListener<UpdateResponse>() {

				@Override
				public void onResponse(UpdateResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Update object {" + uuid + ":" + type + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Exception e) {
					log.error(
							"Updating object {" + uuid + ":" + type + "} to index failed. Duration " + (System.currentTimeMillis() - start) + "[ms]",
							e);
					sub.onError(e);
				}
			});
		});
	}

	@Override
	public Completable storeDocumentBatch(String index, String type, Map<String, JsonObject> documents) {
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
				IndexRequestBuilder indexRequestBuilder = getSearchClient().prepareIndex(index, type, documentId);
				indexRequestBuilder.setSource(document.toString());
				bulk.add(indexRequestBuilder);
			}

			bulk.execute(new ActionListener<BulkResponse>() {
				@Override
				public void onResponse(BulkResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Finished bulk  store request on index {" + index + ":" + type + "}. Duration "
								+ (System.currentTimeMillis() - start) + "[ms]");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Bulk store on index {" + index + ":" + type + "} to index failed. Duration " + (System.currentTimeMillis() - start)
							+ "[ms]", e);
					sub.onError(e);
				}

			});
		});
	}

	@Override
	public Completable storeDocument(String index, String type, String uuid, JsonObject document) {
		return Completable.create(sub -> {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Adding object {" + uuid + ":" + type + "} to index.");
			}
			IndexRequestBuilder builder = getSearchClient().prepareIndex(index, type, uuid);

			builder.setSource(document.toString());
			builder.execute(new ActionListener<IndexResponse>() {

				@Override
				public void onResponse(IndexResponse response) {
					if (log.isDebugEnabled()) {
						log.debug("Added object {" + uuid + ":" + type + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
					}
					sub.onCompleted();
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Adding object {" + uuid + ":" + type + "} to index failed. Duration " + (System.currentTimeMillis() - start) + "[ms]",
							e);
					sub.onError(e);
				}
			});
		});
	}

	@Override
	public Completable deleteIndex(String indexName) {
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
					log.error("Deleting index {" + indexName + "} failed. Duration " + (System.currentTimeMillis() - start) + "[ms]", e);
					sub.onError(e);
				}
			});
		});
	}

	@Override
	public Completable clearIndex(String indexName) {
		return deleteIndex(indexName).andThen(createIndex(indexName));
	}

	@Override
	public Single<Integer> deleteDocumentsViaQuery(String searchQuery, String... indices) {
		return Single.create(sub -> {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("Deleting documents from indices {" + Arrays.toString(indices) + "} via query {" + searchQuery + "}");
			}
			Client client = getNode().client();

			SearchRequestBuilder builder = null;
			try {
				builder = client.prepareSearch(indices).setSource(SearchSourceBuilder.fromXContent(XContentType.JSON.xContent().createParser(NamedXContentRegistry.EMPTY, searchQuery)));
			} catch (IOException e) {
				throw new RuntimeException("Could not parse JSON", e);
			}

			Set<Completable> obs = new HashSet<>();
			builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			builder.execute(new ActionListener<SearchResponse>() {
				@Override
				public void onResponse(SearchResponse response) {

					if (log.isDebugEnabled()) {
						log.debug("Found {" + response.getHits().totalHits + "} which match the deletion query.");
					}
					// Invoke the deletion for each found document
					for (SearchHit hit : response.getHits()) {
						obs.add(deleteDocument(hit.getIndex(), hit.getType(), hit.getId()));
					}
					Completable.merge(obs).await();

					if (log.isDebugEnabled()) {
						log.debug("Deleted {" + obs.size() + "} documents from indices {" + Arrays.toString(indices) + "}");
					}
					sub.onSuccess(obs.size());
				}

				@Override
				public void onFailure(Exception e) {
					log.error("Error deleting from indices {" + Arrays.toString(indices) + "}. Duration " + (System.currentTimeMillis() - start)
							+ "[ms]", e);
					sub.onError(e);
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
		NodesInfoResponse info = getNode().client().admin().cluster().prepareNodesInfo().all().get();
		return getNode().client().admin().cluster().prepareNodesInfo().all().get().getNodes().get(0).getVersion().toString();
	}

}
