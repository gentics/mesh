package com.gentics.mesh.search.index.entry;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.search.CreateIndexEntry;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.Transformer;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.RxHelper;
import rx.Completable;
import rx.Scheduler;

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
	 * Return the index specific transformer which is used to generate the search document and mappings.
	 * 
	 * @return
	 */
	abstract protected Transformer getTransformer();

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
	 * @param element
	 * @param entry
	 *            search queue entry
	 * @return
	 */
	public Completable store(T element, UpdateDocumentEntry entry) {
		String indexName = composeIndexNameFromEntry(entry);
		String documentId = composeDocumentIdFromEntry(entry);
		String indexType = composeIndexTypeFromEntry(entry);
		return searchProvider.storeDocument(indexName, indexType, documentId, getTransformer().toDocument(element)).doOnCompleted(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Stored object in index.");
			}
			searchProvider.refreshIndex();
		});
	}

	@Override
	public Completable updatePermission(UpdateDocumentEntry entry) {
		String uuid = entry.getElementUuid();
		T element = getRootVertex().findByUuid(uuid);
		if (element == null) {
			String type = composeIndexTypeFromEntry(entry);
			throw error(INTERNAL_SERVER_ERROR, "error_element_for_document_type_not_found", uuid, type);
		} else {
			String indexName = composeIndexNameFromEntry(entry);
			String documentId = composeDocumentIdFromEntry(entry);
			String indexType = composeIndexTypeFromEntry(entry);
			return searchProvider.updateDocument(indexName, indexType, documentId, getTransformer().toPermissionPartial(element)).doOnCompleted(
					() -> {
						if (log.isDebugEnabled()) {
							log.debug("Updated object in index.");
						}
						searchProvider.refreshIndex();
					});
		}
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
				T element = getRootVertex().findByUuid(uuid);
				if (element == null) {
					String type = composeIndexTypeFromEntry(entry);
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
			Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());

			return Completable.create(sub -> {

				org.elasticsearch.node.Node node = getESNode();
				PutMappingRequestBuilder mappingRequestBuilder = node.client().admin().indices().preparePutMapping(indexName);
				mappingRequestBuilder.setType(normalizedDocumentType);

				// Generate the mapping for the specific type
				JsonObject mapping = getTransformer().getMapping(normalizedDocumentType);
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
					public void onFailure(Throwable e) {
						sub.onError(e);
					}
				});
			}).observeOn(scheduler);
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
			for (T element : getRootVertex().findAllIt()) {
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
		Set<Completable> obs = new HashSet<>();

		for (String indexKey : indexInfo.keySet()) {
			if (log.isDebugEnabled()) {
				log.debug("Creating index {" + indexKey + "}");
			}
			String documentType = indexInfo.get(indexKey);
			obs.add(searchProvider.createIndex(indexKey).andThen(updateMapping(indexKey, documentType)));
		}
		return Completable.merge(obs);
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

}
