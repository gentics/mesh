package com.gentics.mesh.search.impl;

import static org.elasticsearch.client.Requests.refreshRequest;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.delete.DeleteResponse;
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
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;

import com.gentics.mesh.cli.MeshNameProvider;
import com.gentics.mesh.etc.ElasticSearchOptions;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

/**
 * Elastic search provider class which implements the {@link SearchProvider} interface.
 */
public class ElasticSearchProvider implements SearchProvider {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);

	private Node node;

	private ElasticSearchOptions options;

	@Override
	public void start() {

		if (log.isDebugEnabled()) {
			log.debug("Creating elasticsearch node");
		}
		long start = System.currentTimeMillis();
		ImmutableSettings.Builder elasticsearchSettings = ImmutableSettings.settingsBuilder();
		elasticsearchSettings.put("threadpool.index.queue_size", -1);
		elasticsearchSettings.put("http.enabled", options.isHttpEnabled());
		elasticsearchSettings.put("http.cors.enabled", "true");
		elasticsearchSettings.put("path.data", options.getDirectory());
		elasticsearchSettings.put("node.name", MeshNameProvider.getInstance().getName());
		NodeBuilder builder = NodeBuilder.nodeBuilder();
		// TODO configure ES cluster options
		node = builder.local(true).settings(elasticsearchSettings.build()).node();
		if (log.isDebugEnabled()) {
			log.debug("Waited for elasticsearch shard: " + (System.currentTimeMillis() - start) + "[ms]");
		}
	}

	/**
	 * Initialize and start the search provider using the given options.
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
	public void stop() {
		node.close();
	}

	@Override
	public void refreshIndex() {
		getNode().client().admin().indices().refresh(refreshRequest()).actionGet();
	}

	private Client getSearchClient() {
		return getNode().client();
	}

	@Override
	public Observable<Void> createIndex(String indexName) {
		// TODO Add method which will be used to create an index and set a custom mapping

		return Observable.create(sub -> {
			CreateIndexRequestBuilder createIndexRequestBuilder = getSearchClient().admin().indices().prepareCreate(indexName);
			Map<String, Object> indexSettings = new HashMap<>();
			Map<String, Object> analysisSettings = new HashMap<>();
			Map<String, Object> analyserSettings = new HashMap<>();
			Map<String, Object> defaultAnalyserSettings = new HashMap<>();

			indexSettings.put("analysis", analysisSettings);
			analysisSettings.put("analyzer", analyserSettings);
			analyserSettings.put("default", defaultAnalyserSettings);
			defaultAnalyserSettings.put("type", "standard");
			createIndexRequestBuilder.setSettings(indexSettings);
			createIndexRequestBuilder.execute(new ActionListener<CreateIndexResponse>() {

				@Override
				public void onResponse(CreateIndexResponse response) {
					log.debug("Create index response: {" + response.toString() + "}");
					sub.onNext(null);
					sub.onCompleted();
				}

				@Override
				public void onFailure(Throwable e) {
					if (!(e instanceof IndexAlreadyExistsException)) {
						sub.onError(e);
						log.error("Error while creating index {" + indexName + "}", e);
					} else {
						sub.onNext(null);
						sub.onCompleted();
					}
				}

			});
		});
	}

	@Override
	public Observable<Map<String, Object>> getDocument(String index, String type, String uuid) {
		ObservableFuture<Map<String, Object>> fut = RxHelper.observableFuture();
		getSearchClient().prepareGet(index, type, uuid).execute().addListener(new ActionListener<GetResponse>() {

			@Override
			public void onResponse(GetResponse response) {
				if (log.isDebugEnabled()) {
					log.debug("Get object {" + uuid + ":" + type + "} from index {" + index + "}");
				}
				fut.toHandler().handle(Future.succeededFuture(response.getSourceAsMap()));
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Could not get object {" + uuid + ":" + type + "} from index {" + index + "}");
				fut.toHandler().handle(Future.failedFuture(e));
			}
		});
		return fut;
	}

	@Override
	public Observable<Void> deleteDocument(String index, String type, String uuid) {
		ObservableFuture<Void> fut = RxHelper.observableFuture();
		getSearchClient().prepareDelete(index, type, uuid).execute().addListener(new ActionListener<DeleteResponse>() {

			@Override
			public void onResponse(DeleteResponse response) {
				if (log.isDebugEnabled()) {
					log.debug("Deleted object {" + uuid + ":" + type + "} from index {" + index + "}");
				}
				fut.toHandler().handle(Future.succeededFuture());
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Could not delete object {" + uuid + ":" + type + "} from index {" + index + "}");
				fut.toHandler().handle(Future.failedFuture(e));
			}
		});
		return fut;
	}

	@Override
	public Observable<Void> updateDocument(String index, String type, String uuid, Map<String, Object> map) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Updating object {" + uuid + ":" + type + "} to index.");
		}
		UpdateRequestBuilder builder = getSearchClient().prepareUpdate(index, type, uuid);
		builder.setDoc(map);
		ObservableFuture<Void> fut = RxHelper.observableFuture();
		builder.execute().addListener(new ActionListener<UpdateResponse>() {

			@Override
			public void onResponse(UpdateResponse response) {
				if (log.isDebugEnabled()) {
					log.debug("Update object {" + uuid + ":" + type + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
				fut.toHandler().handle(Future.succeededFuture());
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Updating object {" + uuid + ":" + type + "} to index failed. Duration " + (System.currentTimeMillis() - start) + "[ms]",
						e);
				fut.toHandler().handle(Future.failedFuture(e));
			}
		});
		return fut;
	}

	@Override
	public Observable<Void> storeDocument(String index, String type, String uuid, Map<String, Object> map) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Adding object {" + uuid + ":" + type + "} to index.");
		}
		IndexRequestBuilder builder = getSearchClient().prepareIndex(index, type, uuid);

		builder.setSource(map);
		ObservableFuture<Void> fut = RxHelper.observableFuture();
		builder.execute().addListener(new ActionListener<IndexResponse>() {

			@Override
			public void onResponse(IndexResponse response) {
				if (log.isDebugEnabled()) {
					log.debug("Added object {" + uuid + ":" + type + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
				fut.toHandler().handle(Future.succeededFuture());
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Adding object {" + uuid + ":" + type + "} to index failed. Duration " + (System.currentTimeMillis() - start) + "[ms]", e);
				fut.toHandler().handle(Future.failedFuture(e));
			}
		});
		return fut;

	}

	@Override
	public Observable<Void> deleteIndex(String indexName) {
		long start = System.currentTimeMillis();
		ObservableFuture<Void> fut = RxHelper.observableFuture();
		getSearchClient().admin().indices().prepareDelete(indexName).execute(new ActionListener<DeleteIndexResponse>() {

			public void onResponse(DeleteIndexResponse response) {
				if (log.isDebugEnabled()) {
					log.debug("Deleted index {" + indexName + "}. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
				fut.toHandler().handle(Future.succeededFuture());
			};

			@Override
			public void onFailure(Throwable e) {
				log.error("Deleting index {" + indexName + "} failed. Duration " + (System.currentTimeMillis() - start) + "[ms]", e);
				fut.toHandler().handle(Future.failedFuture(e));
			}
		});
		return fut;
	}

	@Override
	public Observable<Void> clearIndex(String indexName) {
		long start = System.currentTimeMillis();
		ObservableFuture<Void> fut = RxHelper.observableFuture();
		getSearchClient().prepareDeleteByQuery(indexName).setQuery(QueryBuilders.matchAllQuery()).execute()
				.addListener(new ActionListener<DeleteByQueryResponse>() {
					public void onResponse(DeleteByQueryResponse response) {

						if (log.isDebugEnabled()) {
							log.debug("Deleted index {" + indexName + "}. Duration " + (System.currentTimeMillis() - start) + "[ms]");
						}
						fut.toHandler().handle(Future.succeededFuture());
					};

					@Override
					public void onFailure(Throwable e) {
						log.error("Deleting index {" + indexName + "} failed. Duration " + (System.currentTimeMillis() - start) + "[ms]", e);
						fut.toHandler().handle(Future.failedFuture(e));
					}
				});

		return fut;
	}

	@Override
	public Observable<Integer> deleteDocumentsViaQuery(String index, JSONObject query) {
		return deleteDocumentsViaQuery(index, query.toString());
	}

	@Override
	public Observable<Integer> deleteDocumentsViaQuery(String index, String searchQuery) {
		ObservableFuture<Integer> observable = RxHelper.observableFuture();
		Client client = getNode().client();
		SearchRequestBuilder builder = client.prepareSearch(index).setSource(searchQuery);

		Set<Observable<Void>> obs = new HashSet<>();
		builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		builder.execute().addListener(new ActionListener<SearchResponse>() {
			@Override
			public void onResponse(SearchResponse response) {
				for (SearchHit hit : response.getHits()) {
					obs.add(deleteDocument(hit.getIndex(), hit.getType(), hit.getId()));
				}
				observable.toHandler().handle(Future.succeededFuture(obs.size()));
			}

			@Override
			public void onFailure(Throwable e) {
				observable.toHandler().handle(Future.failedFuture(e));
			}
		});

		return observable.delaySubscription(() -> Observable.merge(obs).ignoreElements());
	}
}
