package com.gentics.mesh.search.index;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.elasticsearch.client.okhttp.RequestBuilder;
import com.gentics.madl.tx.Tx;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.MeshEventSender;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.DevNullSearchProvider;
import com.gentics.mesh.search.SearchHandler;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.impl.SearchClient;
import com.gentics.mesh.util.Tuple;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.mapError;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.mapToMeshError;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

/**
 * Abstract implementation for a mesh search handler.
 *
 * @param <T>
 */
public abstract class AbstractSearchHandler<T extends MeshCoreVertex<RM, T>, RM extends RestModel> implements SearchHandler<T, RM> {

	private static final Logger log = LoggerFactory.getLogger(AbstractSearchHandler.class);

	@Inject
	public MeshEventSender meshEventSender;

	@Inject
	public MeshOptions options;

	protected Database db;

	protected SearchProvider searchProvider;

	protected IndexHandler<T> indexHandler;

	public static final long DEFAULT_SEARCH_PER_PAGE = 10;

	/**
	 * Create a new search handler.
	 * 
	 * @param db
	 * @param searchProvider
	 * @param indexHandler
	 */
	public AbstractSearchHandler(Database db, SearchProvider searchProvider, IndexHandler<T> indexHandler) {
		this.db = db;
		this.searchProvider = searchProvider;
		this.indexHandler = indexHandler;
	}

	/**
	 * Prepare the initial search query and inject the values we need to check role permissions.
	 * 
	 * @param ac
	 * @param searchQuery
	 * @param filterLanguage
	 * @return
	 */
	protected JsonObject prepareSearchQuery(InternalActionContext ac, String searchQuery, boolean filterLanguage) {
		try {
			JsonObject userJson = new JsonObject(searchQuery);

			JsonArray roleUuids = new JsonArray();
			try (Tx tx = db.tx()) {
				for (Role role : ac.getUser().getRoles()) {
					roleUuids.add(role.getUuid());
				}
			}
			JsonObject newQuery = new JsonObject().put("bool",
				new JsonObject().put("filter", new JsonArray().add(new JsonObject().put("terms", new JsonObject().put(
					"_roleUuids", roleUuids)))));

			// Wrap the original query in a nested bool query in order check the role perms
			JsonObject originalQuery = userJson.getJsonObject("query");
			if (originalQuery != null) {
				newQuery.getJsonObject("bool").put("must", originalQuery);
				userJson.put("query", newQuery);
			}

			// Add language filter
			if (filterLanguage) {
				List<String> requestedLanguageTags = db.tx(() -> ac.getNodeParameters().getLanguageList());
				if (requestedLanguageTags != null && !requestedLanguageTags.isEmpty()) {
					JsonArray termsFilter = userJson.getJsonObject("query").getJsonObject("bool").getJsonArray("filter");
					termsFilter.add(new JsonObject().put("terms", new JsonObject().put("language", new JsonArray(requestedLanguageTags))));
				}
			}

			return userJson;
		} catch (Exception e) {
			throw new GenericRestException(BAD_REQUEST, "search_query_not_parsable", e);
		}
	}

	@Override
	public void rawQuery(InternalActionContext ac) {
		if (searchProvider instanceof DevNullSearchProvider || searchProvider instanceof TrackingSearchProvider) {
			ac.fail(error(SERVICE_UNAVAILABLE, "search_error_no_elasticsearch_configured"));
			return;
		}

		awaitSync(ac).andThen(Single.defer(() -> {
			SearchClient client = searchProvider.getClient();
			String searchQuery = ac.getBodyAsString();
			if (log.isDebugEnabled()) {
				log.debug("Invoking search with query {" + searchQuery + "}");
			}
			Set<String> indices = indexHandler.getSelectedIndices(ac);

			// Modify the query and add permission checks
			JsonObject request = prepareSearchQuery(ac, searchQuery, false);
			if (log.isDebugEnabled()) {
				log.debug("Using parsed query {" + request.encodePrettily() + "}");
			}

			JsonObject queryOption = new JsonObject();
			queryOption.put("index", StringUtils.join(indices.stream().map(i -> searchProvider.installationPrefix() + i).toArray(String[]::new), ","));
			queryOption.put("search_type", "dfs_query_then_fetch");
			log.debug("Using options {" + queryOption.encodePrettily() + "}");

			RequestBuilder<JsonObject> requestBuilder = client.multiSearch(queryOption, request);
			return requestBuilder.async();
		})).subscribe(response -> {
			// JsonObject firstResponse = response.getJsonArray("responses").getJsonObject(0);
			// Directly relay the response to the requester without converting it.
			ac.send(response.toString(), OK);
		}, error -> {
			if (error instanceof HttpErrorException) {
				HttpErrorException he = (HttpErrorException) error;
				log.error("Search query failed", error);
				log.error("Info: " + error.toString());
				try {
					ac.send(he.getBody(), HttpResponseStatus.BAD_REQUEST);
				} catch (Exception e1) {
					log.error("Error while converting es error to response", e1);
					throw error(INTERNAL_SERVER_ERROR, "Error while converting error.", e1);
				}
			} else if (error instanceof TimeoutException) {
				ac.fail(error(INTERNAL_SERVER_ERROR, "search_error_timeout"));
			} else {
				log.error("Error while handling raw query", error);
				ac.fail(error(INTERNAL_SERVER_ERROR, "search_error_query"));
			}
		});

	}

	@Override
	public <RL extends ListResponse<RM>> void query(InternalActionContext ac, Supplier<RootVertex<T>> rootVertex, Class<RL> classOfRL,
		boolean filterLanguage)
		throws InstantiationException, IllegalAccessException, InvalidArgumentException, MeshJsonException, MeshConfigurationException {
		if (searchProvider instanceof DevNullSearchProvider || searchProvider instanceof TrackingSearchProvider) {
			ac.fail(error(SERVICE_UNAVAILABLE, "search_error_no_elasticsearch_configured"));
			return;
		}

		PagingParameters pagingInfo = ac.getPagingParameters();
		if (pagingInfo.getPage() < 1) {
			throw new InvalidArgumentException("The page must always be positive");
		}
		Long perPage = pagingInfo.getPerPage();
		if (perPage != null && perPage < 0) {
			throw new InvalidArgumentException("The pageSize must always be zero or greater than zero");
		}

		RL listResponse = classOfRL.newInstance();

		awaitSync(ac).andThen(Single.defer(() -> {
			SearchClient client = searchProvider.getClient();
			String searchQuery = ac.getBodyAsString();
			if (log.isDebugEnabled()) {
				log.debug("Invoking search with query {" + searchQuery + "} for {" + classOfRL.getName() + "}");
			}

			Set<String> indices = indexHandler.getSelectedIndices(ac);

			// Add permission checks to the query
			JsonObject request = prepareSearchQuery(ac, searchQuery, filterLanguage);

			// Add paging to query. Internally we start with page 0
			applyPagingParams(request, pagingInfo);

			if (log.isDebugEnabled()) {
				log.debug("Using parsed query {" + request.encodePrettily() + "}");
			}

			JsonObject queryOption = new JsonObject();
			queryOption.put("index", StringUtils.join(indices.stream().map(i -> searchProvider.installationPrefix() + i).toArray(String[]::new), ","));
			queryOption.put("search_type", "dfs_query_then_fetch");
			log.debug("Using options {" + queryOption.encodePrettily() + "}");

			RequestBuilder<JsonObject> requestBuilder = client.multiSearch(queryOption, request);
			return requestBuilder.async();
		})).flatMapObservable(response -> {
			JsonArray responses = response.getJsonArray("responses");
			JsonObject firstResponse = responses.getJsonObject(0);

			// Process the nested error
			JsonObject errorInfo = firstResponse.getJsonObject("error");
			if (errorInfo != null) {
				return Observable.error(mapError(errorInfo));
			}

			JsonObject hitsInfo = firstResponse.getJsonObject("hits");
			JsonArray hits = hitsInfo.getJsonArray("hits");

			List<Tuple<T, String>> list = new ArrayList<>();
			db.tx(() -> {
				for (int i = 0; i < hits.size(); i++) {
					JsonObject hit = hits.getJsonObject(i);
					String id = hit.getString("_id");
					int pos = id.indexOf("-");

					String language = pos > 0 ? id.substring(pos + 1) : null;
					String uuid = pos > 0 ? id.substring(0, pos) : id;

					// Locate the node
					T element = rootVertex.get().findByUuid(uuid);
					if (element == null) {
						log.warn("Object could not be found for uuid {" + uuid + "} in root vertex {" + rootVertex.get().getRootLabel()
							+ "}. The element will be omitted.");
						// Reduce the total count
						hitsInfo.put("total", hitsInfo.getLong("total") - 1);
					} else {
						// TODO maybe it would be better to directly transform the element here.
						list.add(Tuple.tuple(element, language));
					}
				}
			});

			// Set meta information to the rest response
			listResponse.setMetainfo(extractMetaInfo(hitsInfo, pagingInfo));

			return Observable.fromIterable(list);
		}).onErrorResumeNext(error -> {
			return Observable.error(mapToMeshError(error));
		}).flatMapSingle(element -> {
			// TODO add resume next to omit the item if it can't be transformed for some reason.
			// This would be better than to just fail the whole request
			// TODO maybe add extra permission filtering? This would not be very costly for smaller pages and ensure perm consistency?
			// TODO it would be good to batch the transformation of the elements to save the overhead of creating transactions and use the L1 cache.
			return db.tx(() -> Single.just(element.v1().transformToRestSync(ac, 0, element.v2())));
		}).collect(() -> listResponse.getData(), (x, y) -> {
			x.add(y);
		}).subscribe(list -> {
			ac.send(listResponse.toJson(), OK);
		}, error -> {
			log.error("Error while processing search response items", error);
			ac.fail(error);
		});
	}

	private boolean delayRequested(InternalActionContext ac) {
		return ac.getSearchParameters().isWait()
			.orElseGet(options.getSearchOptions()::isWaitForIdle);
	}

	private Completable awaitSync(InternalActionContext ac) {
		if (!delayRequested(ac)) {
			return Completable.complete();
		}

		return meshEventSender.isSearchIdle().flatMapCompletable(isIdle -> {
			if (isIdle) {
				return Completable.complete();
			}
			meshEventSender.flushSearch();
			return meshEventSender.waitForEvent(MeshEvent.SEARCH_IDLE);
		}).andThen(meshEventSender.refreshSearch());
	}

	/**
	 * Add the paging parameters to the request.
	 * 
	 * @param request
	 * @param pagingInfo
	 */
	protected void applyPagingParams(JsonObject request, PagingParameters pagingInfo) {
		long page = pagingInfo.getPage() - 1;
		Long perPage = Optional.ofNullable(pagingInfo.getPerPage()).orElse(DEFAULT_SEARCH_PER_PAGE);
		long low = page * perPage;
		request.put("from", low);
		request.put("size", perPage);
	}

	/**
	 * Extract the total count and hit count from the info object and return the populated paging object. < *
	 * 
	 * @param info
	 * @param pagingInfo
	 * @return
	 */
	protected PagingMetaInfo extractMetaInfo(JsonObject info, PagingParameters pagingInfo) {
		PagingMetaInfo metaInfo = new PagingMetaInfo();
		long total = info.getLong("total");
		metaInfo.setTotalCount(total);
		int totalPages = 0;
		Long perPage = Optional.ofNullable(pagingInfo.getPerPage()).orElse(DEFAULT_SEARCH_PER_PAGE);
		if (perPage != 0) {
			totalPages = (int) Math.ceil(total / (double) perPage);
		}
		// Cap totalpages to 1
		totalPages = totalPages == 0 ? 1 : totalPages;

		metaInfo.setCurrentPage(pagingInfo.getPage());
		metaInfo.setPageCount(totalPages);
		metaInfo.setPerPage(perPage);
		return metaInfo;
	}

	@Override
	public Page<? extends T> query(InternalActionContext ac, String query, PagingParameters pagingInfo, GraphPermission... permissions)
		throws MeshConfigurationException, InterruptedException, ExecutionException, TimeoutException {
		SearchClient client = searchProvider.getClient();
		if (log.isDebugEnabled()) {
			log.debug("Invoking search with query {" + query + "} for {" + indexHandler.getElementClass().getName() + "}");
		}

		Set<String> indices = indexHandler.getSelectedIndices(ac);

		// Add permission checks to the query
		JsonObject queryJson = prepareSearchQuery(ac, query, false);

		// Add paging to query
		applyPagingParams(queryJson, pagingInfo);

		if (log.isDebugEnabled()) {
			log.debug("Using parsed query {" + queryJson.encodePrettily() + "}");
		}

		JsonObject queryOption = new JsonObject();
		queryOption.put("index", StringUtils.join(indices.stream().map(i -> searchProvider.installationPrefix() + i).toArray(String[]::new), ","));
		queryOption.put("search_type", "dfs_query_then_fetch");
		log.debug("Using options {" + queryOption.encodePrettily() + "}");

		// Prepare the request
		RequestBuilder<JsonObject> requestBuilder = client.multiSearch(queryOption, queryJson);
		Single<Page<? extends T>> result = requestBuilder.async()
			.map(response -> {
				JsonArray responses = response.getJsonArray("responses");
				JsonObject firstResponse = responses.getJsonObject(0);

				// Process the nested error
				JsonObject errorInfo = firstResponse.getJsonObject("error");
				if (errorInfo != null) {
					throw mapError(errorInfo);
				}

				return db.tx(() -> {
					List<T> elementList = new ArrayList<>();
					JsonObject hitsInfo = firstResponse.getJsonObject("hits");
					JsonArray hits = hitsInfo.getJsonArray("hits");
					for (int i = 0; i < hits.size(); i++) {
						JsonObject hit = hits.getJsonObject(i);
						String id = hit.getString("_id");
						int pos = id.indexOf("-");
						String uuid = pos > 0 ? id.substring(0, pos) : id;

						// Locate the node
						T element = indexHandler.getRootVertex().findByUuid(uuid);
						if (element != null) {
							elementList.add(element);
						}
					}

					PagingMetaInfo info = extractMetaInfo(hitsInfo, pagingInfo);
					return new PageImpl<>(elementList, info.getTotalCount(), pagingInfo.getPage(), info.getPageCount(), pagingInfo.getPerPage());
				});
			});

		// TODO make this configurable
		return result.timeout(30, TimeUnit.SECONDS)
			.onErrorResumeNext(error -> {
				log.error("Search query failed", error);
				if (error instanceof GenericRestException) {
					return Single.error(error);
				}
				return Single.error(mapToMeshError(error));
			}).blockingGet();
	}

	public IndexHandler<T> getIndexHandler() {
		return indexHandler;
	}

}
