package com.gentics.mesh.search.index.entry;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.search.CreateIndexEntry;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.Transformer;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.reactivex.Completable;

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
	 * Return the index specific transformer which is used to generate the search documents.
	 * 
	 * @return
	 */
	abstract protected Transformer getTransformer();

	/**
	 * Return the index specific mapping provider.
	 * 
	 * @return
	 */
	abstract protected MappingProvider getMappingProvider();

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
		return searchProvider.storeDocument(indexName, documentId, getTransformer().toDocument(element)).doOnComplete(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Stored object in index.");
			}
		});
	}

	@Override
	public Completable updatePermission(UpdateDocumentEntry entry) {
		String uuid = entry.getElementUuid();
		T element = getRootVertex().findByUuid(uuid);
		if (element == null) {
			throw error(INTERNAL_SERVER_ERROR, "error_element_for_document_type_not_found", uuid, DEFAULT_TYPE);
		} else {
			String indexName = composeIndexNameFromEntry(entry);
			String documentId = composeDocumentIdFromEntry(entry);
			return searchProvider.updateDocument(indexName, documentId, getTransformer().toPermissionPartial(element), true).andThen(searchProvider
					.refreshIndex(indexName)).doOnComplete(() -> {
						if (log.isDebugEnabled()) {
							log.debug("Updated object in index.");
						}
					});
		}
	}

	@Override
	public Completable delete(UpdateDocumentEntry entry) {
		String indexName = composeIndexNameFromEntry(entry);
		String documentId = composeDocumentIdFromEntry(entry);
		// We don't need to resolve the uuid and load the graph object in this case.
		return searchProvider.deleteDocument(indexName, documentId);
	}

	@Override
	public Completable store(UpdateDocumentEntry entry) {
		return Completable.defer(() -> {
			try (Tx tx = db.tx()) {
				String uuid = entry.getElementUuid();
				T element = getRootVertex().findByUuid(uuid);
				if (element == null) {
					throw error(INTERNAL_SERVER_ERROR, "error_element_for_document_type_not_found", uuid, DEFAULT_TYPE);
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
		Map<String, IndexInfo> indexInfo = getIndices();
		IndexInfo info = indexInfo.get(indexName);
		// Only create indices which we know of
		if (info != null) {
			// Create the index - Note that dedicated index settings are only configurable for nodes, micronodes (via schema, microschema)
			return searchProvider.createIndex(info);
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Only found indices:");
				for (String idx : indexInfo.keySet()) {
					log.debug("Index name {" + idx + "}");
				}
			}
			throw error(INTERNAL_SERVER_ERROR, "error_index_unknown", indexName);
		}
	}

	@Override
	public Completable init() {
		// Create the indices
		Map<String, IndexInfo> indexInfo = getIndices();
		Set<Completable> obs = new HashSet<>();

		for (IndexInfo info : indexInfo.values()) {
			if (log.isDebugEnabled()) {
				log.debug("Creating index {" + indexInfo + "}");
			}
			obs.add(searchProvider.createIndex(info));
		}
		return Completable.merge(obs);
	}

	@Override
	public boolean accepts(Class<?> clazzOfElement) {
		return getElementClass().isAssignableFrom(clazzOfElement);
	}

}
