package com.gentics.mesh.search.index;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;

/**
 * Abstract class for index handlers.
 * 
 * @see IndexHandler
 */
public abstract class AbstractIndexHandler<T extends MeshCoreVertex<?, T>> implements IndexHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractIndexHandler.class);

	protected SearchProvider searchProvider;

	protected Database db;

	protected BootstrapInitializer boot;

	public AbstractIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot) {
		this.searchProvider = searchProvider;
		this.db = db;
		this.boot = boot;
	}

	/**
	 * Return the index name.
	 * 
	 * @param entry
	 *            entry
	 * 
	 * @return
	 */
	abstract protected String getIndex(SearchQueueEntry entry);

	@Override
	public Map<String, Set<String>> getIndices() {
		Map<String, Set<String>> indexInfo = new HashMap<>();
		indexInfo.put(getIndex(null), Collections.singleton(getDocumentType(null)));
		return indexInfo;
	}

	/**
	 * Extract the search index document type from the entry.
	 * 
	 * @param entry
	 * @return
	 */
	abstract protected String getDocumentType(SearchQueueEntry entry);

	/**
	 * Return the root vertex of the index handler. The root vertex is used to retrieve nodes by UUID in order to update the search index.
	 * 
	 * @return
	 */
	abstract protected RootVertex<T> getRootVertex();

	abstract protected Transformator getTransformator();

	/**
	 * Store the given object within the search index.
	 * 
	 * @param object
	 * @param documentType
	 * @param entry
	 *            search queue entry
	 * @return
	 */
	public Completable store(T object, String documentType, SearchQueueEntry entry) {
		return searchProvider.storeDocument(getIndex(entry), documentType, object.getUuid(), getTransformator().toDocument(object))
				.doOnCompleted(() -> {
					if (log.isDebugEnabled()) {
						log.debug("Stored object in index.");
					}
					MeshInternal.get().searchProvider().refreshIndex();
				});
	}

	@Override
	public Completable delete(SearchQueueEntry entry) {
		// We don't need to resolve the uuid and load the graph object in this case.
		return searchProvider.deleteDocument(getIndex(entry), getDocumentType(entry), entry.getElementUuid());
	}

	@Override
	public Completable store(SearchQueueEntry entry) {
		return Completable.defer(() -> {
			try (NoTx noTx = db.noTx()) {
				String uuid = entry.getElementUuid();
				String type = entry.getElementType();
				T element = getRootVertex().findByUuid(uuid);
				if (element == null) {
					throw error(INTERNAL_SERVER_ERROR, "error_element_for_document_type_not_found", uuid, type);
				} else {
					return store(element, type, entry);
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

	@Override
	public Completable handleAction(SearchQueueEntry entry) {
		String uuid = entry.getElementUuid();
		String actionName = entry.getElementActionName();

		if (!isSearchClientAvailable()) {
			String msg = "Elasticsearch provider has not been initalized. It can't be used. Omitting search index handling!";
			log.error(msg);
			return Completable.error(new Exception(msg));
		}

		if (log.isDebugEnabled()) {
			log.debug("Handling entry {" + entry.toString() + "}");
		}
		SearchQueueEntryAction action = SearchQueueEntryAction.valueOfName(actionName);
		switch (action) {
		case DELETE_ACTION:
			return delete(entry);
		case STORE_ACTION:
			return store(entry);
		case REINDEX_ALL:
			return reindexAll();
		case UPDATE_MAPPING:
			return updateMapping(entry);
		case CREATE_INDEX:
			createIndex(entry).await();
			return Completable.complete();
		default:
			return Completable.error(new Exception("Action type {" + action + "} is unknown."));
		}
	}

	public Completable updateMapping(String indexName, String documentType) {

		if (searchProvider.getNode() != null) {
			PutMappingRequestBuilder mappingRequestBuilder = searchProvider.getNode().client().admin().indices().preparePutMapping(indexName);
			mappingRequestBuilder.setType(documentType);

			JsonObject mapping = getTransformator().getMapping(documentType);

			mappingRequestBuilder.setSource(mapping.toString());

			return Completable.create(sub -> {
				mappingRequestBuilder.execute(new ActionListener<PutMappingResponse>() {

					@Override
					public void onResponse(PutMappingResponse response) {
						if (log.isDebugEnabled()) {
							log.debug("Updated mapping for index {" + indexName + "}");
						}
						sub.onCompleted();
					}

					@Override
					public void onFailure(Throwable e) {
						sub.onError(e);
					}
				});
			});
		} else {
			return Completable.complete();
		}
	}

	@Override
	public Completable updateMapping(SearchQueueEntry entry) {
		String indexName = entry.getElementUuid();
		String documentType = getDocumentType(entry);
		return updateMapping(indexName, documentType);
	}

	@Override
	public Completable reindexAll() {
		return Completable.defer(() -> {
			log.info("Handling full reindex entry");
			SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
			SearchQueueBatch batch = queue.createBatch();
			// Add all elements from the root vertex of the handler to the created batch
			for (T element : getRootVertex().findAll()) {
				log.info("Invoking reindex for {" + element.getType() + "/" + element.getUuid() + "}");
				element.addIndexBatchEntry(batch, STORE_ACTION);
			}
			return batch.processAsync();
		});
	}

	/**
	 * Create the index, if it is one of the indices handled by this index handler. If the index name is not handled by this index handler, an error will be
	 * thrown
	 * 
	 * @param entry
	 *            Search queue entry for create index action
	 * @return
	 */
	protected Completable createIndex(SearchQueueEntry entry) {
		String indexName = entry.getElementUuid();
		Map<String, Set<String>> indexInfo = getIndices();
		if (indexInfo.containsKey(indexName)) {

			// Iterate over all document types of the found index and add completables which will create/update the mapping
			Set<String> documentTypes = indexInfo.get(indexName);
			Set<Completable> mappingObs = new HashSet<>();
			for (String documentType : documentTypes) {
				mappingObs.add(updateMapping(indexName, documentType));
			}
			return searchProvider.createIndex(indexName).andThen(Completable.merge(mappingObs));
		} else {
			throw error(INTERNAL_SERVER_ERROR, "error_index_unknown", indexName);
		}
	}

	@Override
	public Completable init() {
		Map<String, Set<String>> indexInfo = getIndices();
		Set<Completable> indexCreationObs = new HashSet<>();
		indexInfo.keySet().forEach(index -> indexCreationObs.add(searchProvider.createIndex(index)));
		if (indexCreationObs.isEmpty()) {
			return Completable.complete();
		} else {
			Set<Completable> mappingUpdateObs = new HashSet<>();
			for (String indexName : indexInfo.keySet()) {
				for (String documentType : indexInfo.get(indexName)) {
					mappingUpdateObs.add(updateMapping(indexName, documentType));
				}
			}

			return Completable.merge(indexCreationObs).andThen(Completable.merge(mappingUpdateObs));
		}
	}

	@Override
	public Completable clearIndex() {
		Set<Completable> obs = new HashSet<>();
		getIndices().keySet().forEach(index -> obs.add(searchProvider.clearIndex(index)));
		if (obs.isEmpty()) {
			return Completable.complete();
		} else {
			return Completable.merge(obs);
		}
	}
}
