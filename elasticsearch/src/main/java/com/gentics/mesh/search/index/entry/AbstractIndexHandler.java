package com.gentics.mesh.search.index.entry;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.elasticsearch.client.okhttp.RequestBuilder;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.search.CreateIndexEntry;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.bulk.DeleteBulkEntry;
import com.gentics.mesh.core.data.search.bulk.IndexBulkEntry;
import com.gentics.mesh.core.data.search.bulk.UpdateBulkEntry;
import com.gentics.mesh.core.data.search.context.GenericEntryContext;
import com.gentics.mesh.core.data.search.context.impl.GenericEntryContextImpl;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.SearchClient;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.Transformer;
import com.gentics.mesh.search.index.metric.SyncMetric;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract class for index handlers.
 * 
 * @see IndexHandler
 * @param <T>
 *            Type of the elements which are handled by the index handler
 */
public abstract class AbstractIndexHandler<T extends MeshCoreVertex<?, T>> implements IndexHandler<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractIndexHandler.class);

	public static final int ES_SYNC_FETCH_BATCH_SIZE = 1000;

	protected SearchProvider searchProvider;

	protected Database db;

	protected BootstrapInitializer boot;

	public AbstractIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot) {
		this.searchProvider = searchProvider;
		this.db = db;
		this.boot = boot;
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

	public Observable<IndexBulkEntry> storeForBulk(T element, UpdateDocumentEntry entry) {
		String indexName = composeIndexNameFromEntry(entry);
		String documentId = composeDocumentIdFromEntry(entry);
		return Observable
			.just(new IndexBulkEntry(indexName, documentId, getTransformer().toDocument(element)));
	}

	@Override
	public Observable<UpdateBulkEntry> updatePermissionForBulk(UpdateDocumentEntry entry) {
		String uuid = entry.getElementUuid();
		T element = getRootVertex().findByUuid(uuid);
		if (element == null) {
			throw error(INTERNAL_SERVER_ERROR, "error_element_for_document_type_not_found", uuid, DEFAULT_TYPE);
		} else {
			String indexName = composeIndexNameFromEntry(entry);
			String documentId = composeDocumentIdFromEntry(entry);
			return Observable.just(
				new UpdateBulkEntry(indexName, documentId, getTransformer().toPermissionPartial(element)));
		}
	}

	@Override
	public Observable<DeleteBulkEntry> deleteForBulk(UpdateDocumentEntry entry) {
		String indexName = composeIndexNameFromEntry(entry);
		String documentId = composeDocumentIdFromEntry(entry);
		return Observable.just(new DeleteBulkEntry(indexName, documentId));
	}

	@Override
	public Observable<IndexBulkEntry> storeForBulk(UpdateDocumentEntry entry) {
		return Observable.defer(() -> {
			try (Tx tx = db.tx()) {
				String uuid = entry.getElementUuid();
				T element = getRootVertex().findByUuid(uuid);
				if (element == null) {
					throw error(INTERNAL_SERVER_ERROR, "error_element_for_document_type_not_found", uuid, getType());
				} else {
					return storeForBulk(element, entry);
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
	 * Diff the source (graph) with the sink (ES index) and create {@link EventQueueBatch} objects add, delete or update entries.
	 * 
	 * @param indexName
	 * @param projectUuid
	 * @param metric
	 * @return
	 * @throws HttpErrorException
	 */
	protected Completable diffAndSync(String indexName, String projectUuid, SyncMetric metric) throws HttpErrorException {

		log.info("Handling index sync on handler {" + getClass().getName() + "}");

		try (Tx tx = db.tx()) {

			// 1. Load versions from the local graph (source of truth)
			Map<String, String> sourceVersions = loadVersionsFromGraph();

			// 2. Load the version from the elasticsearch index (sink)
			Map<String, String> sinkVersions = loadVersionsFromIndex(indexName);

			// 3. Diff the maps
			MapDifference<String, String> diff = Maps.difference(sourceVersions, sinkVersions);
			if (diff.areEqual()) {
				log.info("No diff detected. Index {" + indexName + "} is in sync.");
				return Completable.complete();
			}

			Set<String> needInsertionInES = diff.entriesOnlyOnLeft().keySet();
			Set<String> needRemovalInES = diff.entriesOnlyOnRight().keySet();
			Set<String> needUpdate = diff.entriesDiffering().keySet();

			log.info("Pending insertions on {" + indexName + "}:" + needInsertionInES.size());
			log.info("Pending removals on {" + indexName + "}:" + needRemovalInES.size());
			log.info("Pending updates on {" + indexName + "}:" + needUpdate.size());

			metric.incInsert(needInsertionInES.size());
			metric.incDelete(needRemovalInES.size());
			metric.incUpdate(needUpdate.size());

			// 4. Create the SQB's
			EventQueueBatch storeBatch = EventQueueBatch.create();
			for (String uuid : needInsertionInES) {
				GenericEntryContext context = new GenericEntryContextImpl();
				context.setProjectUuid(projectUuid);
				UpdateDocumentEntry entry = new UpdateDocumentEntryImpl(this, uuid, context, STORE_ACTION);
				entry.setOnProcessAction(metric::decInsert);
				storeBatch.addEntry(entry);
			}
			EventQueueBatch removalBatch = EventQueueBatch.create();
			for (String uuid : needRemovalInES) {
				GenericEntryContext context = new GenericEntryContextImpl();
				context.setProjectUuid(projectUuid);
				UpdateDocumentEntry entry = new UpdateDocumentEntryImpl(this, uuid, context, DELETE_ACTION);
				entry.setOnProcessAction(metric::decDelete);
				removalBatch.addEntry(entry);
			}
			EventQueueBatch updateBatch = EventQueueBatch.create();
			for (String uuid : needUpdate) {
				GenericEntryContext context = new GenericEntryContextImpl();
				context.setProjectUuid(projectUuid);
				UpdateDocumentEntry entry = new UpdateDocumentEntryImpl(this, uuid, context, STORE_ACTION);
				entry.setOnProcessAction(metric::decUpdate);
				updateBatch.addEntry(entry);
			}

			// 5. Process the SQB's
			return Completable.mergeArray(removalBatch.dispatch(), storeBatch.dispatch(), updateBatch.dispatch());

		}
	}

	private Map<String, String> loadVersionsFromGraph() {
		Map<String, String> versions = new HashMap<>();
		for (T element : getRootVertex().findAll()) {
			String v = generateVersion(element);
			versions.put(element.getUuid(), v);
		}
		return versions;
	}

	public Map<String, String> loadVersionsFromIndex(String indexName) throws HttpErrorException {
		String fullIndexName = searchProvider.installationPrefix() + indexName;
		Map<String, String> versions = new HashMap<>();
		log.debug("Loading document info from index {" + fullIndexName + "}");
		SearchClient client = searchProvider.getClient();
		JsonObject query = new JsonObject();
		query.put("size", ES_SYNC_FETCH_BATCH_SIZE);
		query.put("_source", new JsonArray().add("uuid").add("version"));
		query.put("query", new JsonObject().put("match_all", new JsonObject()));
		query.put("sort", new JsonArray().add("_doc"));

		RequestBuilder<JsonObject> builder = client.searchScroll(query, "1m", fullIndexName);
		JsonObject result = new JsonObject();
		try {
			result = builder.sync();
			if (log.isTraceEnabled()) {
				log.trace("Got response {" + result.encodePrettily() + "}");
			}
			JsonArray hits = result.getJsonObject("hits").getJsonArray("hits");
			processHits(hits, versions);

			// Check whether we need to process more scrolls
			if (hits.size() != 0) {
				String nextScrollId = result.getString("_scroll_id");
				try {
					while (true) {
						final String currentScroll = nextScrollId;
						log.debug("Fetching scroll result using scrollId {" + currentScroll + "}");
						JsonObject scrollResult = client.scroll(currentScroll, "1m").sync();
						JsonArray scrollHits = scrollResult.getJsonObject("hits").getJsonArray("hits");
						if (log.isTraceEnabled()) {
							log.trace("Got response {" + scrollHits.encodePrettily() + "}");
						}
						if (scrollHits.size() != 0) {
							processHits(scrollHits, versions);
							// Update the scrollId for the next fetch
							nextScrollId = scrollResult.getString("_scroll_id");
							if (log.isDebugEnabled()) {
								log.debug("Using scrollId {" + nextScrollId + "} for next fetch.");
							}
						} else {
							// The scroll yields no more data. We are done
							break;
						}
					}
				} finally {
					// Clearing used scroll in order to free memory in ES
					client.clearScroll(nextScrollId).sync();
				}
			}
		} catch (HttpErrorException e) {
			log.error("Error while loading version information from index {" + indexName + "}", e.toString());
			log.error(e);
			throw e;
		}

		return versions;
	}

	protected void processHits(JsonArray hits, Map<String, String> versions) {
		for (int i = 0; i < hits.size(); i++) {
			JsonObject hit = hits.getJsonObject(i);
			JsonObject source = hit.getJsonObject("_source");
			String uuid = source.getString("uuid");
			String version = source.getString("version");
			versions.put(uuid, version);
		}
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
			log.warn("Entry references an unknown index: {}", indexName);
			return Completable.complete();
		}
	}

	@Override
	public Completable init() {
		// Create the indices
		List<Completable> obs = getIndices().values().stream()
			.map(searchProvider::createIndex)
			.peek(info -> {
				if (log.isDebugEnabled()) {
					log.debug("Creating index {" + info + "}");
				}
			})
			.collect(Collectors.toList());

		return Completable.merge(Flowable.fromIterable(obs), 1);
	}

	@Override
	public boolean accepts(Class<?> clazzOfElement) {
		return getElementClass().isAssignableFrom(clazzOfElement);
	}

	@Override
	public String generateVersion(T element) {
		return getTransformer().generateVersion(element);
	}

	@Override
	public Map<String, Object> getMetrics() {
		return SyncMetric.fetch(getType());
	}

	/**
	 * Filter the given indices. Include all indices which start with index handler type and exclude the provided indexName.
	 * 
	 * @param indices
	 * @param indexName
	 * @return
	 */
	protected Set<String> filterIndicesByType(Set<String> indices, String indexName) {
		return indices.stream()
			.filter(i -> i.startsWith(getType()))
			.filter(i -> !i.equals(indexName))
			.collect(Collectors.toSet());
	}

}
