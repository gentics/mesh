package com.gentics.mesh.search.index.entry;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHit;

import com.gentics.ferma.Tx;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.CreateIndexEntry;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.Transformator;
import com.tinkerpop.gremlin.Tokens.T;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import rx.Completable;

/**
 * Abstract class for index handlers.
 * 
 * @see IndexHandler
 * @param <T>
 *            Type of the elements which are handled by the index handler
 */
public abstract class AbstractIndexHandler<T extends MeshCoreVertex<?, T>> implements IndexHandler<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractIndexHandler.class);

	protected SearchProvider searchProvider;

	protected Database db;

	protected BootstrapInitializer boot;

	private SearchQueue searchQueue;

	public AbstractIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		this.searchProvider = searchProvider;
		this.db = db;
		this.boot = boot;
		this.searchQueue = searchQueue;
	}

	/**
	 * Return the root vertex of the index handler. The root vertex is used to retrieve nodes by UUID in order to update the search index.
	 * 
	 * @return
	 */
	abstract protected RootVertex<T> getRootVertex();

	/**
	 * Return the index specific transformator which is used to generate the search document and mappings.
	 * 
	 * @return
	 */
	abstract protected Transformator getTransformator();

	/**
	 * Return the class of elements which can be handled by this handler.
	 * 
	 * @return
	 */
	abstract protected Class<?> getElementClass();

	/**
	 * Compose the index name using the batch entry data.
	 * 
	 * @param entry
	 * @return
	 */
	abstract protected String composeIndexNameFromEntry(UpdateDocumentEntry entry);

	/**
	 * Compose the document id using the batch entry data.
	 * 
	 * @param entry
	 * @return
	 */
	abstract protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry);

	/**
	 * Compose the index type using the batch entry data.
	 * 
	 * @param entry
	 * @return
	 */
	abstract protected String composeIndexTypeFromEntry(UpdateDocumentEntry entry);

	/**
	 * Store the given object within the search index.
	 * 
	 * @param object
	 * @param entry
	 *            search queue entry
	 * @return
	 */
	public Completable store(T object, UpdateDocumentEntry entry) {
		String indexName = composeIndexNameFromEntry(entry);
		String documentId = composeDocumentIdFromEntry(entry);
		String indexType = composeIndexTypeFromEntry(entry);
		return searchProvider.storeDocument(indexName, indexType, documentId, getTransformator().toDocument(object)).doOnCompleted(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Stored object in index.");
			}
			searchProvider.refreshIndex();
		});
	}

	@Override
	public Completable delete(UpdateDocumentEntry entry) {
		String indexName = composeIndexNameFromEntry(entry);
		String typeId = composeIndexTypeFromEntry(entry);
		String documentId = composeDocumentIdFromEntry(entry);
		// We don't need to resolve the uuid and load the graph object in this case.
		return searchProvider.deleteDocument(indexName, typeId, documentId);
	}

	@Override
	public Completable store(UpdateDocumentEntry entry) {
		return Completable.defer(() -> {
			try (Tx tx = db.tx()) {
				String uuid = entry.getElementUuid();
				String type = composeIndexTypeFromEntry(entry);
				T element = getRootVertex().findByUuid(uuid);
				if (element == null) {
					throw error(INTERNAL_SERVER_ERROR, "error_element_for_document_type_not_found", uuid, type);
				} else {
					return store(element, entry);
				}
			}
		});
	}

	/**
	 * Check whether the search provider is available. Some tests are not starting an search provider and thus we must be able to determine whether we can use
	 * the search provider.
	 * 
	 * @return
	 */
	protected boolean isSearchClientAvailable() {
		return searchProvider != null;
	}

	/**
	 * Update the index and type specific mapping. This method will use the implementation specific transformer in order to generate the needed mappings.
	 * 
	 * @param indexName
	 * @param documentType
	 * @return
	 */
	public Completable updateMapping(String indexName, String documentType) {

		final String normalizedDocumentType = documentType.toLowerCase();
		if (searchProvider.getNode() != null) {
			return Completable.create(sub -> {

				org.elasticsearch.node.Node node = getESNode();
				PutMappingRequestBuilder mappingRequestBuilder = node.client().admin().indices().preparePutMapping(indexName);
				mappingRequestBuilder.setType(normalizedDocumentType);

				// Generate the mapping for the specific type
				JsonObject mapping = getTransformator().getMapping(normalizedDocumentType);
				mappingRequestBuilder.setSource(mapping.toString());

				mappingRequestBuilder.execute(new ActionListener<PutMappingResponse>() {

					@Override
					public void onResponse(PutMappingResponse response) {
						if (log.isDebugEnabled()) {
							log.debug("Updated mapping for index {" + indexName + "}");
						}
						sub.onCompleted();
					}

					@Override
					public void onFailure(Exception e) {
						sub.onError(e);
					}
				});
			});
		} else {
			return Completable.complete();
		}
	}

	/**
	 * Utility method that is used to return the elasticsearch node.
	 * 
	 * @return es node
	 * @throws RuntimeException
	 *             Exception which will be thrown if the ES node can't be returned
	 */
	protected org.elasticsearch.node.Node getESNode() {
		// Fetch the elastic search instance
		if (searchProvider.getNode() != null && searchProvider.getNode() instanceof org.elasticsearch.node.Node) {
			return (org.elasticsearch.node.Node) searchProvider.getNode();
		} else {
			throw new RuntimeException("Unable to get elasticsearch instance from search provider got {" + searchProvider.getNode() + "}");
		}
	}

	@Override
	public Completable updateMapping(CreateIndexEntry entry) {
		String indexName = entry.getIndexName();
		String documentType = entry.getIndexType();
		return updateMapping(indexName, documentType);
	}

	@Override
	public Completable reindexAll() {
		return Completable.defer(() -> {
			log.info("Handling full reindex entry");
			SearchQueueBatch batch = searchQueue.create();
			// Add all elements from the root vertex of the handler to the created batch
			for (T element : getRootVertex().findAll()) {
				if (element instanceof IndexableElement) {
					IndexableElement indexableElement = (IndexableElement) element;
					log.info("Invoking reindex in handler {" + getClass().getName() + "} for element {" + indexableElement.getUuid() + "}");
					batch.store(indexableElement, false);
				} else {
					log.info("Found element {" + element.getUuid() + "} is not indexable. Ignoring element.");
				}
			}
			return batch.processAsync();
		});
	}

	@Override
	public Completable createIndex(CreateIndexEntry entry) {
		String indexName = entry.getIndexName();
		Map<String, String> indexInfo = getIndices();
		if (indexInfo.containsKey(indexName)) {
			// Iterate over all document types of the found index and add
			// completables which will create/update the mapping
			String documentType = indexInfo.get(indexName);
			Set<Completable> obs = new HashSet<>();
			obs.add(updateMapping(indexName, documentType));
			return searchProvider.createIndex(indexName).andThen(Completable.merge(obs));
		} else {
			throw error(INTERNAL_SERVER_ERROR, "error_index_unknown", indexName);
		}
	}

	@Override
	public Completable init() {
		// 1. Create the indices
		Map<String, String> indexInfo = getIndices();
		Set<Completable> indexCreationObs = new HashSet<>();

		for (String indexKey : indexInfo.keySet()) {
			if (log.isDebugEnabled()) {
				log.debug("Creating index {" + indexKey + "}");
			}
			indexCreationObs.add(searchProvider.createIndex(indexKey));
		}
		if (indexCreationObs.isEmpty()) {
			return Completable.complete();
		} else {
			// 2. Create the mappings
			Set<Completable> mappingUpdateObs = new HashSet<>();
			for (String indexName : indexInfo.keySet()) {
				String documentType = indexInfo.get(indexName);
				mappingUpdateObs.add(updateMapping(indexName, documentType));
			}
			return Completable.merge(indexCreationObs).andThen(Completable.merge(mappingUpdateObs));
		}
	}

	@Override
	public Completable clearIndex() {
		Set<Completable> obs = new HashSet<>();
		// Iterate over all indices which the handler is responsible for and
		// clear all of them.
		getIndices().keySet().forEach(index -> obs.add(searchProvider.clearIndex(index)));
		if (obs.isEmpty()) {
			return Completable.complete();
		} else {
			return Completable.merge(obs);
		}
	}

	@Override
	public boolean accepts(Class<?> clazzOfElement) {
		return getElementClass().isAssignableFrom(clazzOfElement);
	}

	@Override
	public Page<? extends T> query(InternalActionContext ac, String query, PagingParameters pagingInfo, GraphPermission... permissions)
			throws MeshConfigurationException, InterruptedException, ExecutionException, TimeoutException {
		User user = ac.getUser();

		org.elasticsearch.node.Node esNode = null;
		if (searchProvider.getNode() instanceof org.elasticsearch.node.Node) {
			esNode = (org.elasticsearch.node.Node) searchProvider.getNode();
		} else {
			throw new MeshConfigurationException("Unable to get elasticsearch instance from search provider got {" + searchProvider.getNode() + "}");
		}
		Client client = esNode.client();

		if (log.isDebugEnabled()) {
			log.debug("Invoking search with query {" + query + "} for {" + getElementClass().getName() + "}");
		}

		/*
		 * TODO, FIXME This a very crude hack but we need to handle paging ourself for now. In order to avoid such nasty ways of paging a custom ES plugin has
		 * to be written that deals with Document Level Permissions/Security (commonly known as DLS)
		 */
		SearchRequestBuilder builder = null;
		try {
			JSONObject queryStringObject = new JSONObject(query);
			/**
			 * Note that from + size can not be more than the index.max_result_window index setting which defaults to 10,000. See the Scroll API for more
			 * efficient ways to do deep scrolling.
			 */
			queryStringObject.put("from", 0);
			queryStringObject.put("size", Integer.MAX_VALUE);
			Set<String> indices = getSelectedIndices(ac);
			builder = client.prepareSearch(indices.toArray(new String[indices.size()])).setSource(SearchSourceBuilder.fromXContent(XContentType.JSON.xContent().createParser(NamedXContentRegistry.EMPTY, query)));
		} catch (Exception e) {
			throw new GenericRestException(BAD_REQUEST, "search_query_not_parsable", e);
		}
		CompletableFuture<Page<? extends T>> future = new CompletableFuture<>();
		builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		builder.execute(new ActionListener<SearchResponse>() {

			@Override
			public void onResponse(SearchResponse response) {
				Page<? extends T> page = db.tx(() -> {
					List<T> elementList = new ArrayList<T>();
					for (SearchHit hit : response.getHits()) {

						String id = hit.getId();
						int pos = id.indexOf("-");
						String uuid = pos > 0 ? id.substring(0, pos) : id;

						// TODO check permissions without loading the vertex

						// Locate the node
						T element = getRootVertex().findByUuid(uuid);
						if (element != null) {
							// Check permissions and language
							for (GraphPermission permission : permissions) {
								if (user.hasPermission(element, permission)) {
									elementList.add(element);
									break;
								}
							}
						}
					}
					Page<? extends T> elementPage = Page.applyPaging(elementList, pagingInfo);
					return elementPage;
				});
				future.complete(page);
			}

			@Override
			public void onFailure(Exception e) {
				log.error("Search query failed", e);
				future.completeExceptionally(e);
			}
		});

		return future.get(60, TimeUnit.SECONDS);

	}

}
