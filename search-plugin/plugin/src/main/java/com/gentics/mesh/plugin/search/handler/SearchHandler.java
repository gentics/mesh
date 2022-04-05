package com.gentics.mesh.plugin.search.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.extension.CustomHandler;
import com.gentics.mesh.plugin.search.extension.HitTransformer;
import com.gentics.mesh.plugin.search.service.BranchService;
import com.gentics.mesh.plugin.search.service.QueryService;
import com.gentics.mesh.plugin.search.util.CollectorUtils;
import com.gentics.mesh.plugin.search.util.JsonUtils;
import com.gentics.mesh.plugin.search.util.QueryBuilderUtils;
import com.gentics.mesh.plugin.search.util.RequestUtils;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.util.NumberUtils;
import com.gentics.vertx.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handler for normal search reqests.
 */
@Singleton
public class SearchHandler extends AbstractQueryHandler {

	/**
	 * A collector intended to be used for {@link Observable#collectInto(Object, io.reactivex.functions.BiConsumer)}
	 * which will add the emitted items to a provided list.
	 *
	 * <p>
	 * The {@link #indices} passed to the constructor will be used to determine the order in which
	 * the elements are inserted in the list. For this to work the initial List must already have a size
	 * big enough to fit all emitted elements.
	 * </p>
	 *
	 * <p>
	 * When the elements have a field named <code>uuid</code> the elements will be placed in the list according
	 * to the indices specified in <code>indices</code>. Otherwise they will just be added in the order they
	 * are emitted.
	 * </p>
	 */
	private class ResultCollector implements BiConsumer<List<JsonObject>, JsonObject> {

		/** The indices the items should be placed at. */
		private final Map<String, Integer> indices;

		/** Whether the check for the <code>uuid</code> was already done. */
		private boolean checkedResults = false;
		/** Whether the emitted items have a <code>uuid</code> field and can be sorted according to the {@link #indices}. */
		private boolean haveIds = false;

		/**
		 * Default constructor.
		 *
		 * @param indices The desired order of items in the resulting list
		 */
		ResultCollector(Map<String, Integer> indices) {
			this.indices = indices;
		}

		/*
		 * (non-Javadoc)
		 * @see io.reactivex.functions.BiConsumer#accept(java.lang.Object, java.lang.Object)
		 */
		@Override
		public void accept(List<JsonObject> list, JsonObject item) {
			if (!checkedResults) {
				checkedResults = true;
				haveIds = item.containsKey("branch")
					&& item.containsKey("uuid")
					&& item.containsKey("language");

				if (!haveIds) {
					log.warn("Search result query did not contain UUIDs, final result will not be sorted");

					// The result list was prefilled with null values so that we can List.set() the items at the correct
					// position instead of sorting the list afterwards. Since we cannot guarantee sorting anyways, and use
					// List.add() to add the items need to remove those null values.
					list.clear();
				}
			}

			if (haveIds) {
				String key = createHitKey(item, item.getString("branch"));

				if (indices.containsKey(key)) {
					list.set(indices.get(key), item);
				}
			} else {
				list.add(item);
			}
		}
	}

	private static final String RS_TOTAL_FIELD = "totalHits";
	private static final String RS_HITS_FIELD = "results";
	private static final String RS_PAGE_HITCOUNT_FIELD = "pageHitCount";
	private static final String ES_HITS_FIELD = "hits";
	private static final String ES_SOURCE_FIELD = "_source";
	private static final String ES_BRANCH_UUID_FIELD = "branchUuid";
	private static final String ES_BRANCH_UUID_PATH = ES_SOURCE_FIELD + "/" + ES_BRANCH_UUID_FIELD;
	private static final String ES_PROJECT_NAME_PATH = ES_SOURCE_FIELD + "/project/name";


	private final QueryService queryService;
	private final BranchService branchService;

	/** The hit transformer responsible for filling the search result JSON object. */
	private final Provider<HitTransformer> hitTransformerProvider;

	private final String resultQuery;

	@Inject
	public SearchHandler(
			SearchConfiguration config,
			PluginContext context,
			QueryService queryService,
			BranchService branchService,
			Provider<HitTransformer> hitTransformerProvider,
			@Nullable CustomHandler customHandler) {
		super(config, context);

		this.queryService = queryService;
		this.branchService = branchService;
		this.hitTransformerProvider = hitTransformerProvider;

		if (customHandler == null) {
			init(queryService::getSearchQuery);
			this.resultQuery = config.getResultQuery();
		} else {
			init(customHandler);
			this.resultQuery = customHandler.getResultQueryName();
		}
	}

	/**
	 * Perform the actual Elasticsearch query and transform the result.
	 *
	 * @see #transformResult(JsonObject)
	 * @see #transformHits(JsonArray)
	 * @see #finalizeHits(JsonObject, List)
	 * @see #addPagingInfo(JsonObject, String)
	 * @see #addDidYouMean(JsonObject)
	 *
	 * @return A <code>Completable</code> for the handler action
	 */
	@Override
	public Completable internalHandle() {
		String project = context.projectName();
		HttpServerRequest request = context.request();
		String queryParam = request.getParam("q");

		if (queryParam == null) {
			context.fail(HttpResponseStatus.BAD_REQUEST.code());

			return Completable.error(new Exception("Missing parameter \"q\""));
		}

		return querySupplier.get()
			.doOnSuccess(this::injectBranchUuidSource)
			.doOnSuccess(this::traceQuery)
			.flatMap(query -> RequestUtils.getSearchRequest(context, query.encode(), multiProjectSearch, waitForIdle).toSingle())
			.map(JsonUtils::jsonObjectOf)
			.flatMap(this::transformResult)
			.flatMap(result -> transformHits(result.getJsonArray(RS_HITS_FIELD)).flatMap(hits -> finalizeHits(result, hits)))
			.flatMap(result -> addPagingInfo(result, queryParam))
			.flatMap(this::addDidYouMean)
			.flatMapCompletable(resultMapper)
			.doOnError(context::fail);
	}

	/**
	 * Add the {@code branchUuid} to the {@code _source} array if the array is
	 * present.
	 *
	 * @param query The original Elasticsearch query
	 */
	private void injectBranchUuidSource(JsonObject query) {
		JsonArray source = query.getJsonArray(ES_SOURCE_FIELD);

		if (source != null && !source.contains(ES_BRANCH_UUID_FIELD)) {
			log.trace("Injecting \"branchUuid\" into \"_source\"");

			source.add(ES_BRANCH_UUID_FIELD);
		}
	}

	/**
	 * Extracts the raw search hits and the aggregated tags from the Elasticsearch query.
	 *
	 * @see #transformTags(JsonObject, List)
	 *
	 * @param input The result from the elastic search query
	 * @return A Single with a JSON object containing aggregated tags and the raw search hits
	 */
	private Single<JsonObject> transformResult(JsonObject input) {
		if (log.isTraceEnabled()) {
			log.trace("Query response:\n{}", input.encodePrettily());
		}

		List<String> activeTags = QueryBuilderUtils.getTags(context.request());
		JsonObject output = new JsonObject();
		JsonArray responses = input.getJsonArray("responses");

		if (responses == null || responses.isEmpty()) {
			log.error("No responses from Elasticsearch query, enable TRACE for more details");

			return Single.just(output);
		}

		JsonObject result = responses.getJsonObject(0);
		JsonObject hits = result.getJsonObject(ES_HITS_FIELD);

		if (hits == null) {
			log.error("No hits in Elasticsearch response, enable TRACE for more details");

			return Single.just(output);
		}

		JsonArray pageHits = hits.getJsonArray(ES_HITS_FIELD, new JsonArray());

		// We just pass on the raw Elasticsearch hits for now, they will be processed later.
		output.put(RS_HITS_FIELD, pageHits);
		// We need to know how many hits Elasticsearch found, even if the GraphQL query will reduce this later.
		output.put(RS_TOTAL_FIELD, getTotalHits(hits));
		output.put(RS_PAGE_HITCOUNT_FIELD, pageHits.size());

		JsonObject aggregations = result.getJsonObject("aggregations", new JsonObject());

		if (aggregations.isEmpty()) {
			log.trace("Search result did not contain aggregations");
		} else {
			aggregations.forEach(agg -> {
				JsonObject tmp = (JsonObject) agg.getValue();
				// if json array has doc_count then treat this as a global aggregation
				if (tmp.containsKey("doc_count")) {
					tmp.stream().forEach(entry -> {
						String key = entry.getKey();
						if (!key.equals("doc_count")) {
							JsonObject val = (JsonObject) entry.getValue();
							JsonArray t = transformTags(val, activeTags);
							output.put(key, t);
						}
					});
				} else {
					JsonArray tags = transformTags((JsonObject) agg.getValue(), activeTags);

					output.put(agg.getKey(), tags);
				}

			});
		}

		return Single.just(output);
	}

	/**
	 * Get the number of total hits from the search result.
	 *
	 * <p>
	 *     Before Elasticsearch 7 the respective field in the response was just an integer. Since version 7.0.0 it
	 *     is an object containing
	 *     <ul>
	 *         <li>value: the number of hits</li>
	 *         <li>relation: "eq" or "gte" when "value" is exact or a lower bound respectively</li>
	 *     </ul>
	 * </p>
	 *
	 * <p>
	 *     As of now "relation" will always be "eq", so we just ignore it.
	 * </p>
	 *
	 * @param hits The search hits to get the total from
	 * @return The number of total hits
	 */
	private int getTotalHits(JsonObject hits) {
		Object total = hits.getValue("total");

		if (total instanceof Integer) {
			return (Integer) total;
		} else if (total instanceof JsonObject) {
			// TODO Indicate that the total is just a lower bound when Elasticsearch implements this. (As of now the
			//  relation will always be "eq".)
			return ((JsonObject) total).getInteger("value");
		}

		log.warn("Could not determine total hits");

		return 0;
	}

	/**
	 * Transform the buckets to an array of JsonObjects mapping <code>key</code> to <code>doc_count</code>
	 *
	 * <p>
	 * Tags already specified in the current search request will <em>not</em> be included in the result.
	 * </p>
	 *
	 * @param aggregation The aggregation result to transform
	 * @param activeTags The tags specified for the current search request
	 * @return A JsonArray containing JsonObject mapping the bucket keys to their respective <code>doc_count</code>
	 */
	private JsonArray transformTags(JsonObject aggregation, List<String> activeTags) {
		JsonArray buckets = aggregation.getJsonArray("buckets", new JsonArray());
		JsonArray ret = new JsonArray();

		for (Object o : buckets) {
			if (o instanceof JsonObject) {
				JsonObject bucket = (JsonObject) o;
				String tag = bucket.getString("key", "").trim();
				int count = bucket.getInteger("doc_count", 0);

				if (!tag.isEmpty() && !activeTags.contains(tag) && count > 0) {
					ret.add(new JsonObject().put(tag, count));
				}
			}
		}

		return ret;
	}

	/**
	 * Load additional information for the hits if necessary.
	 *
	 * <p>
	 * When no {@link #resultQuery result query} was specified this method just returns a
	 * <code>Single</code> with the original Elasticsearch hits. Otherwise the configured query will be executed.
	 * </p>
	 *
	 * <p>
	 * In both cases the results will be passed to the {@link HitTransformer#transform(JsonObject, JsonObject, String, BranchResponse) hit transformer},
	 * but when not query is configured, the <code>node</code> and <code>branch</code> parameters will be <code>null</code>.
	 * </p>
	 *
	 * @param hits The Elasticsearch hits
	 * @return A <code>Single</code> containing the Elastisearch hits when no result query is configured, and the
	 * 		result of that query otherwise
	 */
	private Single<List<JsonObject>> transformHits(JsonArray hits) {
		if (StringUtils.isBlank(resultQuery)) {
			log.debug("No result query defined, returning raw search result");

			List<JsonObject> result = hits.stream()
				.map(o -> hitTransformerProvider.get().transform((JsonObject) o, null, context.projectName(), null))
				.collect(Collectors.toList());

			return Single.just(result);
		}

		List<Observable<JsonObject>> queryResults = new ArrayList<>();
		Map<String, Integer> indices = hits.stream()
			// The ID of a search hit has the form "[Node UUID]-[Language code]".
			.map(o -> createHitKey((JsonObject) o, null))
			.collect(CollectorUtils.mapToIndex());

		hits.stream()
			.map(o -> (JsonObject) o)
			.collect(Collectors.groupingBy(this::getBranchUuid))
			.forEach((key, val) -> queryResults.add(loadResultItems(key, val)));

		return Observable.merge(queryResults)
			.collectInto(
				new ArrayList<>(Collections.nCopies(indices.size(), null)),
				new ResultCollector(indices));
	}

	/**
	 * Create a key for identifying a raw Elasticsearch hit.
	 *
	 * <p>
	 * Those keys always have the form <code>BRANCH_UUID-NODE_UUID-LANGUAGE_CODE</code>.
	 * </p>
	 *
	 * <p>
	 * When no <code>branch</code> is provided, the <code>hit</code> is assumed to come from Elasticsearch. In this case
	 * the branch UUID will be extracted from the <code>_index</code> field, and node UUID and language are taken from
	 * the <code>_id</code> field.
	 * </p>
	 *
	 * <p>
	 * When a <code>branch</code> is provided, the <code>hit</code> is assumed to be a GraphQL result. Then the key will
	 * be constructed from the provided <code>branch</code> as well as the <code>uuid</code> and <code>language</code>
	 * fields from the <code>hit</code>.
	 * </p>
	 *
	 * @param hit The Elasticsearch <em>or</em> GraphQL query result
	 * @param branch The branch the GraphQL hit originates from, or <code>null</code> when <code>hit</code> is an
	 * 		Elasticsearch hit
	 * @return The key for identifying an Elasticsearch hit
	 */
	private String createHitKey(JsonObject hit, String branch) {
		if (branch == null) {
			// When no branch is provided we assume it is an Elasticsearch hit.
			return getBranchUuid(hit) + "-" + hit.getString("_id");
		}

		return branch + "-" + hit.getString("uuid") + "-" + hit.getString("language");
	}

	/**
	 * Get the branch UUID from the <code>hit</code>s index name.
	 *
	 * @param hit The Elasticsearch hit
	 * @return The UUID of the hits branch
	 */
	private String getBranchUuid(JsonObject hit) {
		String branchUuid = JsonUtils.getValue(hit, ES_BRANCH_UUID_PATH, null);

		if (StringUtils.isNotBlank(branchUuid)) {
			log.trace("Found branch UUID in search hit");

			return branchUuid;
		}

		log.trace("Extracting branch UUID from index name");

		/*
		 * The index name schema looks like:
		 * :indexPrefix-:projectUuid-:branchUuid-:schemaVersionUuid-:versionType[-:language]
		 *
		 * The -:language part at the end is optional, and only present, when
		 * there are language specific elasticsearch settings. The index prefix
		 * can change, so just counting the number of '-' is not very stable.
		 *
		 * Ideally we could get the branch UUID from something else than the
		 * index name, but this requires additional changes how Mesh indices
		 * data.
		 */
		String[] idxParts = hit.getString("_index").split("-");

		// Version type is either "draft" or "published" and is last when there
		// are no language specific settings.
		String last = idxParts[idxParts.length - 1];
		int offset = last.equals("published") || last.equals("draft") ? 3 : 4;

		return idxParts[idxParts.length - offset];
	}

	/**
	 * Executes the {@link SearchConfiguration#getResultQuery() result query} for the hits in the specified branch.
	 *
	 * <p>
	 * The results of the GraphQL query will be passed to {@link #toSearchHits(String, BranchResponse, Map, GraphQLResponse)}
	 * for post-processing.
	 * </p>
	 *
	 * @see #toSearchHits(String, BranchResponse, Map, GraphQLResponse)
	 *
	 * @param branchUuid The branch UUID for the items in <code>hits</code>
	 * @param hits The items for which additional information should be loaded
	 *
	 * @return An <code>Observable</code> containing the final search hits as JSON objects
	 */
	private Observable<JsonObject> loadResultItems(String branchUuid, List<JsonObject> hits) {
		if (log.isDebugEnabled()) {
			log.debug("Loading data for {} hit{} in branch {}", hits.size(), hits.size() == 1 ? "" : "s", branchUuid);
		}

		if (hits.isEmpty()) {
			return Observable.empty();
		}

		String projectName = JsonUtils.getValue(hits.get(0), ES_PROJECT_NAME_PATH, "");
		Map<String, JsonObject> rawHits = hits.stream().collect(
			// The ID of a search hit has the form "[Node UUID]-[Language code]".
			Collectors.toMap(h -> createHitKey(h, null), Function.identity()));


		String langParam = context.request().getParam("l");
		// TODO make default language configurable?
		String lang = StringUtils.isBlank(langParam) ? "en" : langParam;
		VersioningParameters version = new VersioningParametersImpl().published().setBranch(branchUuid);

		List<String> uuids = rawHits.values().stream().map(this::getUuidFromHit).collect(Collectors.toList());
		JsonObject variables = new JsonObject()
			.put("lang", lang)
			.put("uuids", uuids);

		// Load additional info that is not in the index via GraphQL (e.g. breadcrumb, path, ...).
		return queryService.getSearchResultQuery(projectName, resultQuery)
			.doOnSuccess(query -> {
				if (log.isDebugEnabled()) {
					log.debug("Loading search results for [{}] ({})", String.join(", ", uuids), lang);
				}

				if (log.isTraceEnabled()) {
					log.trace("Using GrapqhQL query:\n{}", query);
				}

			})
			.map(query -> new GraphQLRequest().setQuery(query).setVariables(variables))
			.flatMapObservable(request -> context.client().graphql(projectName, request, version).toObservable())
			.flatMap(rs -> branchService.getBranch(projectName, branchUuid)
				.flatMapObservable(branch -> toSearchHits(projectName, branch, rawHits, rs)));
	}

	/**
	 * Extract the Mesh UUID from the <code>_id</code> field of an Elasticsearch hit.
	 *
	 * @param hit The Elasticsearch hit
	 * @return The UUID of the corresponding object in Mesh
	 */
	private String getUuidFromHit(JsonObject hit) {
		String id = hit.getString("_id");

		return id.substring(0, id.lastIndexOf('-'));
	}

	/**
	 * Extracts the <code>uuid</code> and <code>url</code> from each hit, and passes it to the
	 * {@link HitTransformer#transform(JsonObject, JsonObject, String, BranchResponse) hit transformer}.
	 *
	 * @param projectName The project the <code>hits</code> are from
	 * @param branch The branch of the <code>hits</code>
	 * @param hits The search hits to post-process
	 * @param rs The result of the GraphQL query
	 *
	 * @return An <code>Observable</code> with the final search results
	 */
	private Observable<JsonObject> toSearchHits(String projectName, BranchResponse branch, Map<String, JsonObject> hits, GraphQLResponse rs) {
		JsonObject data = rs.getData();

		if (data == null) {
			log.warn("Empty response for UUIDs [{}] in branch {} ({})", String.join(", ", hits.keySet()), branch.getUuid(), projectName);

			return Observable.empty();
		}

		if (log.isDebugEnabled()) {
			log.debug("Received data for {} hit{} in branch {} ({})", hits.size(), hits.size() == 1 ? "" : "s", branch.getUuid(), projectName);
		}

		if (log.isTraceEnabled()) {
			log.trace("GraphQL response:\n{}", data.encodePrettily());
		}

		List<JsonObject> ret = new ArrayList<>();
		JsonArray elements = JsonUtils.getValue(data, "nodes/elements", new JsonArray());

		if (elements.size() != hits.size()) {
			log.warn("Found only {} of {} element{} in branch {} ({})", elements.size(), hits.size(), elements.size() == 1 ? "" : "s", branch.getUuid(), projectName);
		}

		elements.stream()
			.map(o -> (JsonObject) o)
			.flatMap(e -> e.getJsonArray("languages").stream().map(o -> (JsonObject) o))
			.forEach(hit -> {
				String key = createHitKey(hit, branch.getUuid());

				if (hits.containsKey(key)) {
					JsonObject rawHit = hits.get(key);
					JsonObject item = new JsonObject()
						.put("branch", branch.getUuid())
						.put("uuid", hit.getString("uuid"))
						.put("language", hit.getString("language"))
						.put("url", getUrl(projectName, branch, hit.getString("path")))
						.mergeIn(hitTransformerProvider.get().transform(rawHit, hit, context.projectName(), branch));

					ret.add(item);
				} else {
					log.warn("No Elasticsearch hit found for key \"{}\"", key);
					log.debug("This may be caused by a missing language filter in the Elasticsearch query");
				}
			});

		return Observable.fromIterable(ret);
	}

	/**
	 * Build a URL to an object with the specified <code>path</code> using the hostname information
	 * from the given <code>branch</code>.
	 *
	 * @param projectName The project to build a URL for
	 * @param branch The branch information to be used for the URL
	 * @param path The actual path to an object
	 *
	 * @return A URL to the specified path for the given branch
	 */
	private String getUrl(String projectName, BranchResponse branch, String path) {
		if (context.projectName().equals(projectName)) {
			return path;
		}

		return "http" + (Boolean.TRUE.equals(branch.getSsl()) ? "s" : "") + "://" + branch.getHostname() + path;
	}

	/**
	 * Add paging information to the search result.
	 *
	 * @param result The search result
	 * @param query The original search query
	 * @return A <code>Single</code> with the <code>result</code> with added paging information
	 * @throws UnsupportedEncodingException When the <code>query</code> cannot be URL encoded
	 */
	private Single<JsonObject> addPagingInfo(JsonObject result, String query) throws UnsupportedEncodingException {
		int currentPage = NumberUtils.toInt(context.request().getParam("p"), 0) + 1;

		// We can only approximate the total hits count, because it is possible that we do not get
		// results from GraphQL for some elasticsearch hits.
		long missing = result.getInteger(RS_PAGE_HITCOUNT_FIELD) - result.getJsonArray(RS_HITS_FIELD).size();
		long total = result.getInteger(RS_TOTAL_FIELD) - missing;
		boolean hasMore = total > currentPage * config.getPageSize();

		result.remove(RS_PAGE_HITCOUNT_FIELD);
		result.put(RS_TOTAL_FIELD, total);
		result.put("hasMore", hasMore);

		if (hasMore) {
			result.put("nextBatchUrl", "/search?q=" + URLEncoder.encode(query, "UTF-8") + "&p=" + currentPage);
		}

		return Single.just(result);
	}

	/**
	 * Filters unwanted elements from the <code>hits</code> and puts them into the final <code>result</code>.
	 *
	 * @param result The final search result
	 * @param hits The search hits
	 * @return A <code>Single</code> with the final search result
	 */
	private Single<JsonObject> finalizeHits(JsonObject result, List<JsonObject> hits) {
		// This can still happen if the Elasticsearch query does not use the %lang% parameter to filter languages. In
		// this case the result from Elasticsearch could contain a hit for a language, that is not loaded by the following
		// GraphQL query.
		hits.removeIf(Objects::isNull);

		result.put(RS_HITS_FIELD, hits);

		return Single.just(result);
	}

	/**
	 * Add did-you-mean suggestions when viewer than {@link SearchConfiguration#getDidYouMeanThreshold() didYouMeanThreshold}
	 * hits were found.
	 *
	 * @see #mergeSuggestions(JsonObject, JsonObject)
	 * @see #transformDidYouMean(JsonObject)
	 *
	 * @param result The search result to add suggestions to if necessary
	 *
	 * @return A <code>Single</code> which contains just the passed <code>result</code> when enough hits were
	 * 		found, and a <code>Single</code> containing the <code>result</code> where the suggestions have
	 * 		been added otherwise
	 */
	private Single<JsonObject> addDidYouMean(JsonObject result) {
		int total = result.getInteger("totalHits", 0);

		if (total < config.getDidYouMeanThreshold()) {
			if (log.isDebugEnabled()) {
				log.debug("Found only {}/{} hit(s), requesting suggestions", total, config.getDidYouMeanThreshold());
			}

			return Observable.fromIterable(config.getSearchedProjects(context.projectName()))
				.flatMap(this::getDidYouMeanRequests)
				.map(JsonUtils::jsonObjectOf)
				.reduce(new JsonObject(), this::mergeSuggestions)
				.map(s -> result.put("didYouMean", transformDidYouMean(s)));
		}

		return Single.just(result);
	}

	/**
	 * Get the did-you-mean requests for the specified project.
	 *
	 * @param project The project to get suggestions for
	 * @return An <code>Observable</code> containing requests to loadValue content and binary did-you-mean
	 * 		requests for the given project
	 */
	private Observable<ObjectNode> getDidYouMeanRequests(String project) {
		log.debug("Loading suggestions for project {}", project);

		MeshRestClient client = context.client();
		VersioningParameters version = new VersioningParametersImpl().published();
		String branch = context.queryParams().get("branch");

		if (!StringUtils.isBlank(branch)) {
			version.setBranch(branch);
		}

		return queryService.getDidYouMeanQuery(context, false).toObservable()
			.mergeWith(queryService.getDidYouMeanQuery(context, true).toObservable())
			.flatMap(q -> client.searchNodesRaw(project, q.encode(), version).toObservable());
	}

	/**
	 * Merges the suggestions in <code>response</code> into the <code>options</code> field of the
	 * <code>accumulator</code>.
	 *
	 * @param accumulator The <code>JsonObject</code> in which all suggestions are gathered
	 * @param response The Elasticsearch response with potential new suggestions
	 * @return The <code>accumulator</code> with the suggestions from <code>response</code> added
	 */
	private JsonObject mergeSuggestions(JsonObject accumulator, JsonObject response) {
		JsonArray responses = response.getJsonArray("responses");

		if (responses == null || responses.isEmpty()) {
			return accumulator;
		}

		JsonObject suggestions = responses.getJsonObject(0).getJsonObject("suggest");

		if (suggestions == null) {
			if (log.isTraceEnabled()) {
				log.trace("No suggestions in response:\n{}", response.encodePrettily());
			}

			return accumulator;
		}

		if (log.isTraceEnabled()) {
			log.trace("Merging suggestions:\n{}", suggestions.encodePrettily());
		}

		if (!accumulator.containsKey("options")) {
			accumulator.put("options", new JsonArray());
		}

		JsonArray options = accumulator.getJsonArray("options");

		suggestions.stream()
			.flatMap(e -> ((JsonArray) e.getValue()).stream().flatMap(o -> ((JsonObject) o).getJsonArray("options").stream()))
			.forEach(options::add);

		if (log.isTraceEnabled()) {
			log.trace("Accumulated options:\n{}", accumulator.getJsonArray("options").encodePrettily());
		}

		return accumulator;
	}

	/**
	 * Extracts the did-you-mean phrases from the input string and returns the one with the highest score.
	 *
	 * <p>
	 * When the <code>collate_match</code> field is present for an option, it must be <code>true</code>,
	 * or the option will be ignored.
	 * </p>
	 *
	 * @param input Result of the Elasticsearch query
	 *
	 * @return Found phrase or null if no phrase could be found
	 */
	private String transformDidYouMean(JsonObject input) {
		BinaryOperator<JsonObject> bestScore = (result, element) -> {
			boolean match = element.getBoolean("collate_match", true);
			boolean betterScore = result == null || result.getDouble("score").compareTo(element.getDouble("score")) < .0;

			return match && betterScore ? element : result;
		};

		if (log.isTraceEnabled()) {
			log.trace("Processing did you mean suggestions:\n{}", input.encodePrettily());
		}

		JsonObject bestOption = input.getJsonArray("options", new JsonArray()).stream()
			.map(e -> (JsonObject) e)
			.reduce(null, bestScore);

		return bestOption == null ? null : bestOption.getString("text");
	}
}
