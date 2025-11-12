package com.gentics.mesh.search.index.entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.elasticsearch.client.okhttp.RequestBuilder;
import com.gentics.mesh.context.SimpleDataHolderContext;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.search.EntityMetrics;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.DataHolderContext;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.metric.SyncMeters;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Abstract class for index handlers.
 * 
 * @see IndexHandler
 * @param <T>
 *            Type of the elements which are handled by the index handler
 */
public abstract class AbstractIndexHandler<T extends HibBaseElement> implements IndexHandler<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractIndexHandler.class);

	public static final int ES_SYNC_FETCH_BATCH_SIZE = 10_000;

	protected final SearchProvider searchProvider;

	protected final Database db;

	protected final MeshHelper helper;

	protected final MeshOptions options;

	protected final ComplianceMode complianceMode;

	protected final SyncMeters meters;

	protected final BucketManager bucketManager;

	public AbstractIndexHandler(SearchProvider searchProvider, Database db, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetersFactory, BucketManager bucketManager) {
		this.searchProvider = searchProvider;
		this.db = db;
		this.helper = helper;
		this.options = options;
		this.complianceMode = options.getSearchOptions().getComplianceMode();
		this.meters = syncMetersFactory.createSyncMetric(getType());
		this.bucketManager = bucketManager;
	}

	@Override
	abstract public MappingProvider getMappingProvider();

	/**
	 * Check whether the search provider is available. Some tests are not starting an search provider and thus we must be able to determine whether we can use
	 * the search provider.
	 * 
	 * @return
	 */
	protected boolean isSearchClientAvailable() {
		return searchProvider != null;
	}

	protected Flowable<SearchRequest> diffAndSync(String indexName, String projectUuid, Optional<Pattern> indexPattern) {
		if (indexPattern.orElse(MATCH_ALL).matcher(indexName).matches()) {
			// Sync each bucket individually
			Flowable<Bucket> buckets = bucketManager.getBuckets(getTotalCountFromGraph());
			log.debug("Handling index sync on handler: {}", getClass().getName());
			return buckets.flatMap(bucket -> {
				log.info("Handling sync of {" + bucket + "}");
				return diffAndSync(indexName, projectUuid, bucket);
			}, 1);
		} else {
			return Flowable.empty();
		}
	}

	/**
	 * Diff the source (graph) with the sink (ES index) and create {@link EventQueueBatch} objects add, delete or update entries.
	 * 
	 * @param indexName
	 * @param projectUuid
	 * @param bucket
	 * @return
	 */
	protected Flowable<SearchRequest> diffAndSync(String indexName, String projectUuid, Bucket bucket) {
		DataHolderContext dhc = new SimpleDataHolderContext();
		return Single.zip(
			loadVersionsFromIndex(indexName, bucket),
			Single.fromCallable(() -> loadVersionsFromStorage(bucket, dhc)),
			(sinkVersions, sourceVersions) -> {
				log.debug("Handling index sync on handler {" + getClass().getName() + "} for {" + bucket + "}");
				log.debug("Found {" + sourceVersions.size() + "} elements in graph bucket");
				log.debug("Found {" + sinkVersions.size() + "} elements in search index bucket");

				MapDifference<String, String> diff = Maps.difference(sourceVersions, sinkVersions);
				if (diff.areEqual()) {
					log.debug("No diff detected. Index {" + indexName + "} is in sync.");
					return Flowable.<SearchRequest>empty();
				}

				Set<String> needInsertionInES = diff.entriesOnlyOnLeft().keySet();
				Set<String> needUpdateInEs = diff.entriesDiffering().keySet();
				Set<String> needRemovalInES = diff.entriesOnlyOnRight().keySet();

				log.info("Pending insertions on {" + indexName + "}: " + needInsertionInES.size());
				log.info("Pending removals on {" + indexName + "}: " + needRemovalInES.size());

				meters.getInsertMeter().addPending(needInsertionInES.size());
				meters.getUpdateMeter().addPending((needUpdateInEs.size()));
				meters.getDeleteMeter().addPending((needRemovalInES.size()));

				Function<String, Optional<JsonObject>> toDoc = uuid -> {
					return db.tx(() -> {
						T element = getElement(uuid);
						if (element == null) {
							log.warn("Element for uuid {" + uuid + "} in type handler {" + getType() + "}  could not be found. Skipping document.");
							return Optional.empty();
						} else {
							return Optional.of(getTransformer().toDocument(element, dhc));
						}
					});
				};

				Function<Action, Function<JsonObject, CreateDocumentRequest>> toCreateRequest = action -> doc -> {
					String uuid = doc.getString("uuid");
					return helper.createDocumentRequest(indexName, uuid, doc, complianceMode, action);
				};

				Flowable<SearchRequest> toInsert = Flowable.fromIterable(needInsertionInES)
					.map(toDoc).filter(opt -> opt.isPresent()).map(opt -> opt.get())
					.map(toCreateRequest.apply(meters.getInsertMeter()::synced));

				Flowable<SearchRequest> toUpdate = Flowable.fromIterable(needUpdateInEs)
					.map(toDoc).filter(opt -> opt.isPresent()).map(opt -> opt.get())
					.map(toCreateRequest.apply(meters.getUpdateMeter()::synced));

				Flowable<SearchRequest> toDelete = Flowable.fromIterable(needRemovalInES)
					.map(uuid -> helper.deleteDocumentRequest(indexName, uuid, complianceMode, meters.getDeleteMeter()::synced));

				return Flowable.merge(toInsert, toUpdate, toDelete);
			}).flatMapPublisher(x -> x);
	}

	protected T getElement(String elementUuid) {
		return elementLoader().apply(elementUuid);
	}

	private Map<String, String> loadVersionsFromStorage(Bucket bucket, DataHolderContext dhc) {
		return db.tx(tx -> {
			Collection<? extends T> elementsOfBucket = loadAllElements(bucket, dhc);
			return elementsOfBucket.stream()
					.collect(Collectors.toMap(HibBaseElement::getUuid, e -> generateVersion(e, dhc)));
		});
	}

	/**
	 * Load a map of versions and documentIds from the given search index. Apply the bucket parameters to the query in order to only select documents within the
	 * bucket range.
	 * 
	 * @param indexName
	 * @param bucket
	 * @return
	 */
	// TODO Async
	public Single<Map<String, String>> loadVersionsFromIndex(String indexName, Bucket bucket) {
		return Single.fromCallable(() -> {
			String fullIndexName = searchProvider.installationPrefix() + indexName;
			Map<String, String> versions = new HashMap<>();
			log.trace("Loading document info from index {" + fullIndexName + "} in bucket {" + bucket + "}");

			ElasticsearchClient<JsonObject> client = searchProvider.getClient();
			JsonObject query = new JsonObject();
			query.put("size", ES_SYNC_FETCH_BATCH_SIZE);
			query.put("_source", new JsonArray().add("uuid").add("version"));
			query.put("query", bucket.rangeQuery());
			query.put("sort", new JsonArray().add("_doc"));

			log.debug("Using {} query:\n\t", fullIndexName, query.encodePrettily());
			RequestBuilder<JsonObject> builder = client.searchScroll(query, "1m", fullIndexName);
			JsonObject result = new JsonObject();

			// collect all scroll IDs
			Set<String> scrollIds = new HashSet<>();

			try {
				result = builder.sync();
				log.debug("Got response:\n{}", result.encodePrettily());
				Optional.ofNullable(result.getString("_scroll_id")).ifPresent(scrollIds::add);
				JsonArray hits = result.getJsonObject("hits", new JsonObject()).getJsonArray("hits", new JsonArray());
				processHits(hits, versions);

				try {
					// Check whether we need to process more scrolls
					if (hits.size() != 0) {
						String nextScrollId = result.getString("_scroll_id");
						while (true) {
							final String currentScroll = nextScrollId;
							log.debug("Fetching scroll result using scrollId {}", currentScroll);
							JsonObject scrollResult = client.scroll("1m", currentScroll).sync();
							Optional.ofNullable(scrollResult.getString("_scroll_id")).ifPresent(scrollIds::add);
							JsonArray scrollHits = scrollResult.getJsonObject("hits").getJsonArray("hits");
							log.debug("Got response:\n\t[{}]", scrollHits.encodePrettily());
							if (scrollHits.size() != 0) {
								processHits(scrollHits, versions);
								// Update the scrollId for the next fetch
								nextScrollId = scrollResult.getString("_scroll_id");
								log.debug("Using scrollId [{}] for next fetch.", nextScrollId);
							} else {
								// The scroll yields no more data. We are done
								break;
							}
						}
					}
				} finally {
					if (!scrollIds.isEmpty()) {
						JsonObject scrollIdBody = new JsonObject();
						scrollIdBody.put("scroll_id", new JsonArray(new ArrayList<String>(scrollIds)));
						try {
							client.clearScroll(scrollIdBody).sync();
						} catch (Throwable e) {
							log.error("Error while clearing open scrolls");
						}
					}
				}
			} catch (HttpErrorException e) {
				log.error("Could not load versions of index " + indexName);
				throw e;
			}
			return versions;
		});
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
	public Completable init() {
		// Create the indices
		return Observable.defer(() -> Observable.fromIterable(getIndices().entrySet()))
			.flatMap(entry -> entry.getValue().map(info -> searchProvider.createIndex(info)).orElseGet(() -> searchProvider.deleteIndex(false, entry.getKey())).toObservable()
				.doOnSubscribe(ignore -> {
					if (log.isDebugEnabled()) {
						log.debug(entry.getValue().map(yes -> "Creating").orElseGet(() -> "Dropping") + " index {" + entry.getKey() + "} " + entry.getValue().map(Object::toString).orElse(StringUtils.EMPTY));
					}
				}), 1)
			.ignoreElements();
	}

	// @Override
	// public Flowable<SearchRequest> init() {
	// return Flowable.defer(() -> Flowable.fromIterable(getIndices().values()))
	// .map(CreateIndexRequest::new);
	// }

	@Override
	public boolean accepts(Class<?> clazzOfElement) {
		return getElementClass().isAssignableFrom(clazzOfElement);
	}

	@Override
	public String generateVersion(T element, DataHolderContext dhc) {
		return getTransformer().generateVersion(element, dhc);
	}

	@Override
	public EntityMetrics getMetrics() {
		return meters.createSnapshot();
	}

	@Override
	public Completable check() {
		// check the indices
		return Observable.defer(() -> Observable.fromIterable(getIndices().entrySet()))
				.flatMap(entry -> entry.getValue().map(info ->  searchProvider.check(info)).orElseGet(Completable::complete)
					.toObservable()
					.doOnSubscribe(ignore -> {
						if (log.isDebugEnabled()) {
							log.debug(entry.getValue().map(yes -> "Checking").orElseGet(() -> "Skipping") + " index {" + entry.getKey() + "} " + entry.getValue().map(Object::toString).orElse(StringUtils.EMPTY));
						}
					}), 1)
				.ignoreElements();
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
