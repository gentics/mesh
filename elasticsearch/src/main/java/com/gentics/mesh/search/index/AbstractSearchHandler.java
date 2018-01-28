package com.gentics.mesh.search.index;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.DevNullSearchProvider;
import com.gentics.mesh.search.SearchHandler;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.impl.SearchClient;
import com.gentics.mesh.util.Tuple;
import com.syncleus.ferma.tx.Tx;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract implementation for a mesh search handler.
 *
 * @param <T>
 */
public abstract class AbstractSearchHandler<T extends MeshCoreVertex<RM, T>, RM extends RestModel> implements SearchHandler<T, RM> {

	private static final Logger log = LoggerFactory.getLogger(AbstractSearchHandler.class);

	protected Database db;

	protected SearchProvider searchProvider;

	protected IndexHandler<T> indexHandler;

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
	 * @return
	 */
	protected JsonObject prepareSearchQuery(InternalActionContext ac, String searchQuery) {
		JsonObject userJson = new JsonObject(searchQuery);

		JsonArray roleUuids = new JsonArray();
		try (Tx tx = db.tx()) {
			for (Role role : ac.getUser().getRoles()) {
				roleUuids.add(role.getUuid());
			}
		}

		JsonObject newQuery = new JsonObject().put("bool",
				new JsonObject().put("filter", new JsonObject().put("terms", new JsonObject().put("_roleUuids", roleUuids))));

		// Wrap the original query in a nested bool query in order check the role perms
		JsonObject originalQuery = userJson.getJsonObject("query");
		if (originalQuery != null) {
			newQuery.getJsonObject("bool").put("must", originalQuery);
			userJson.put("query", newQuery);
		}

		if (userJson.getLong("size") == null) {
			userJson.put("size", 8000);
		}

		// TODO filter the node language for a node search request

		return userJson;
	}

	@Override
	public void rawQuery(InternalActionContext ac) {
		if (searchProvider instanceof DevNullSearchProvider || searchProvider instanceof TrackingSearchProvider) {
			ac.fail(error(SERVICE_UNAVAILABLE, "search_error_no_elasticsearch_configured"));
			return;
		}

		SearchClient client = searchProvider.getClient();
		String searchQuery = ac.getBodyAsString();
		if (log.isDebugEnabled()) {
			log.debug("Invoking search with query {" + searchQuery + "}");
		}
		Set<String> indices = indexHandler.getSelectedIndices(ac);

		// Modify the query and add permission checks
		JsonObject request = prepareSearchQuery(ac, searchQuery);
		if (log.isDebugEnabled()) {
			log.debug("Using parsed query {" + request.encodePrettily() + "}");
		}

		// Setup the request
		// ?search_type=dfs_query_then_fetch

		client.queryAsync(request, indices.toArray(new String[indices.size()])).subscribe(response -> {
			ac.send(response.toString(), OK);
		}, error -> {
			if (error instanceof HttpErrorException) {
				HttpErrorException he = (HttpErrorException) error;
				log.error("Search query failed", error);
				try {
					ac.send(he.getBody(), HttpResponseStatus.BAD_REQUEST);
				} catch (Exception e1) {
					log.error("Error while converting es error to response", e1);
					throw error(INTERNAL_SERVER_ERROR, "Error while converting error.", e1);
				}
			} else {
				// ac.fail(new GenericRestException(BAD_REQUEST, "search_query_not_parsable", e));
				// ac.fail(error(INTERNAL_SERVER_ERROR, "search_error_timeout"));
			}
		});

	}

	@Override
	public <RL extends ListResponse<RM>> void query(InternalActionContext ac, Supplier<RootVertex<T>> rootVertex, Class<RL> classOfRL)
			throws InstantiationException, IllegalAccessException, InvalidArgumentException, MeshJsonException, MeshConfigurationException {
		if (searchProvider instanceof DevNullSearchProvider || searchProvider instanceof TrackingSearchProvider) {
			ac.fail(error(SERVICE_UNAVAILABLE, "search_error_no_elasticsearch_configured"));
			return;
		}

		PagingParameters pagingInfo = ac.getPagingParameters();
		if (pagingInfo.getPage() < 1) {
			throw new InvalidArgumentException("The page must always be positive");
		}
		if (pagingInfo.getPerPage() < 0) {
			throw new InvalidArgumentException("The pageSize must always be zero or greater than zero");
		}

		RL listResponse = classOfRL.newInstance();
		SearchClient client = searchProvider.getClient();
		String searchQuery = ac.getBodyAsString();
		if (log.isDebugEnabled()) {
			log.debug("Invoking search with query {" + searchQuery + "} for {" + classOfRL.getName() + "}");
		}

		Set<String> indices = indexHandler.getSelectedIndices(ac);
		JsonObject request = prepareSearchQuery(ac, searchQuery);
		// Add permission checks to the query
		if (log.isDebugEnabled()) {
			log.debug("Using parsed query {" + request.encodePrettily() + "}");
		}

		// ?search_type=dfs_query_then_fetch
		client.queryAsync(request, indices.toArray(new String[indices.size()])).map(response -> {
			db.tx(() -> {
				List<Single<Tuple<T, String>>> obs = new ArrayList<>();
				List<String> requestedLanguageTags = ac.getNodeParameters().getLanguageList();

				for (Object o : response.getJsonArray("hits")) {
					JsonObject hit = (JsonObject) o;
					String id = hit.getString("id");
					int pos = id.indexOf("-");

					String language = pos > 0 ? id.substring(pos + 1) : null;
					String uuid = pos > 0 ? id.substring(0, pos) : id;

					// Locate the node
					T element = rootVertex.get().findByUuid(uuid);
					if (element == null) {
						log.error("Object could not be found for uuid {" + uuid + "} in root vertex {" + rootVertex.get().getRootLabel() + "}");
					} else {
						obs.add(Single.just(Tuple.tuple(element, language)));
					}
				}

				Single.merge(obs).collect(() -> {
					return new ArrayList<Tuple<T, String>>();
				}, (x, y) -> {
					if (y == null) {
						return;
					}
					// TODO it would be better to filter the request language within the ES query instead. This way we could also use native paging from ES.
					// Check whether the language matches up
					boolean matchesRequestedLang = y.v2() == null || requestedLanguageTags == null || requestedLanguageTags.isEmpty()
							|| requestedLanguageTags.contains(y.v2());
					if (y != null && matchesRequestedLang) {
						x.add(y);
					}
				}).subscribe(list -> {
					// Internally we start with page 0
					int page = pagingInfo.getPage() - 1;

					int low = page * pagingInfo.getPerPage();
					int upper = low + pagingInfo.getPerPage() - 1;

					int n = 0;
					List<Single<RM>> transformedElements = new ArrayList<>();
					for (Tuple<T, String> objectAndLanguageTag : list) {

						// Only transform elements that we want to list in our resultset
						if (n >= low && n <= upper) {
							// Transform node and add it to the list of nodes
							transformedElements.add(objectAndLanguageTag.v1().transformToRest(ac, 0, objectAndLanguageTag.v2()));
						}
						n++;
					}

					// Set meta information to the rest response
					PagingMetaInfo metainfo = new PagingMetaInfo();
					int totalPages = 0;
					if (pagingInfo.getPerPage() != 0) {
						totalPages = (int) Math.ceil(list.size() / (double) pagingInfo.getPerPage());
					}
					// Cap totalpages to 1
					totalPages = totalPages == 0 ? 1 : totalPages;
					metainfo.setTotalCount(list.size());
					metainfo.setCurrentPage(pagingInfo.getPage());
					metainfo.setPageCount(totalPages);
					metainfo.setPerPage(pagingInfo.getPerPage());
					listResponse.setMetainfo(metainfo);

					List<Observable<RM>> obsList = transformedElements.stream().map(ele -> ele.toObservable()).collect(Collectors.toList());
					// Populate the response data with the transformed elements and send the response
					Observable.concat(Observable.fromIterable(obsList)).collect(() -> {
						return listResponse.getData();
					}, (x, y) -> {
						x.add(y);
					}).subscribe(itemList -> {
						ac.send(JsonUtil.toJson(listResponse), OK);
					}, error -> {
						ac.fail(error);
					});

				}, error -> {
					log.error("Error while processing search response items", error);
					ac.fail(error);
				});
				return null;
			});
		}).doOnError(error -> {

			// if (log.isDebugEnabled()) {
			// log.debug("Query failed {" + searchQuery + "}");
			// }
			// ac.fail(new GenericRestException(BAD_REQUEST, "search_query_not_parsable", e));

			if (error instanceof TimeoutException) {
				ac.fail(error(INTERNAL_SERVER_ERROR, "search_error_timeout"));
			} else if (e instanceof ElasticsearchException) {
				GenericRestException error = error(BAD_REQUEST, "search_error_query", simpleMessage(e));
				log.error("Search query failed", e);
				int i = 0;
				for (Throwable e1 = e.getCause(); e1 != null; e1 = e1.getCause()) {
					error.setProperty("cause-" + i, simpleMessage(e1));
					i++;
				}
				ac.fail(error);
			}
		});

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
		JsonObject queryJson = prepareSearchQuery(ac, query);
		if (log.isDebugEnabled()) {
			log.debug("Using parsed query {" + queryJson.encodePrettily() + "}");
		}
		try {
			// Prepare the request
			// ?search_type=dfs_query_then_fetch
		} catch (Exception e) {
			throw new GenericRestException(BAD_REQUEST, "search_query_not_parsable", e);
		}
		CompletableFuture<Page<? extends T>> future = new CompletableFuture<>();
		client.queryAsync(queryJson, indices.toArray(new String[indices.size()])).map(response -> {
			Page<? extends T> page = db.tx(() -> {
				List<T> elementList = new ArrayList<T>();
				for (Object o : response.getJsonObject("hits").getJsonArray("hits")) {
					JsonObject hit = (JsonObject) o;
					String id = hit.getString("id");
					int pos = id.indexOf("-");
					String uuid = pos > 0 ? id.substring(0, pos) : id;

					// Locate the node
					T element = indexHandler.getRootVertex().findByUuid(uuid);
					if (element != null) {
						elementList.add(element);
					}
				}
				Page<? extends T> elementPage = Page.applyPaging(elementList, pagingInfo);
				return elementPage;
			});
			future.complete(page);
		}).doOnError(error -> {
			log.error("Search query failed", e);
			future.completeExceptionally(e);
		});
		return future.get(60, TimeUnit.SECONDS);
	}

	public IndexHandler<T> getIndexHandler() {
		return indexHandler;
	}

}
