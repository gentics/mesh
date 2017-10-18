package com.gentics.mesh.search;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import rx.Observable;
import rx.Single;
import rx.functions.Func0;

/**
 * Collection of handlers which are used to deal with rest search requests.
 */
public class SearchRestHandler {

	private static final Logger log = LoggerFactory.getLogger(SearchRestHandler.class);

	private SearchProvider searchProvider;

	private Database db;

	private IndexHandlerRegistry registry;

	private NodeIndexHandler nodeIndexHandler;

	private HandlerUtilities utils;

	@Inject
	public SearchRestHandler(SearchProvider searchProvider, Database db, IndexHandlerRegistry registry, NodeIndexHandler nodeIndexHandler,
			HandlerUtilities utils) {
		this.searchProvider = searchProvider;
		this.db = db;
		this.registry = registry;
		this.nodeIndexHandler = nodeIndexHandler;
		this.utils = utils;
	}

	/**
	 * Handle a search request.
	 * 
	 * @param ac
	 * @param rootVertex
	 *            Root Vertex of the elements that should be searched
	 * @param classOfRL
	 *            Class of the rest model list that should be used when creating the response
	 * @param indices
	 *            Names of indices which should be searched
	 * @param permission
	 *            required permission
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvalidArgumentException
	 * @throws MeshJsonException
	 * @throws MeshConfigurationException
	 */
	public <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void handleSearch(InternalActionContext ac,
			Func0<RootVertex<T>> rootVertex, Class<RL> classOfRL, Set<String> indices, GraphPermission permission)
			throws InstantiationException, IllegalAccessException, InvalidArgumentException, MeshJsonException, MeshConfigurationException {

		PagingParameters pagingInfo = ac.getPagingParameters();
		if (pagingInfo.getPage() < 1) {
			throw new InvalidArgumentException("The page must always be positive");
		}
		if (pagingInfo.getPerPage() < 0) {
			throw new InvalidArgumentException("The pageSize must always be zero or greater than zero");
		}

		RL listResponse = classOfRL.newInstance();
		MeshAuthUser requestUser = ac.getUser();

		org.elasticsearch.node.Node esNode = null;
		if (searchProvider.getNode() instanceof org.elasticsearch.node.Node) {
			esNode = (Node) searchProvider.getNode();
		} else {
			throw new MeshConfigurationException("Unable to get elasticsearch instance from search provider got {" + searchProvider.getNode() + "}");
		}
		Client client = esNode.client();

		String searchQuery = ac.getBodyAsString();
		if (log.isDebugEnabled()) {
			log.debug("Invoking search with query {" + searchQuery + "} for {" + classOfRL.getName() + "}");
		}

		/*
		 * TODO, FIXME This a very crude hack but we need to handle paging ourself for now. In order to avoid such nasty ways of paging a custom ES plugin has
		 * to be written that deals with Document Level Permissions/Security (commonly known as DLS)
		 */
		SearchRequestBuilder builder = null;
		try {
			JSONObject queryStringObject = new JSONObject(searchQuery);
			/**
			 * Note that from + size can not be more than the index.max_result_window index setting which defaults to 10,000. See the Scroll API for more
			 * efficient ways to do deep scrolling.
			 */
			queryStringObject.put("from", 0);
			queryStringObject.put("size", Integer.MAX_VALUE);
			builder = client.prepareSearch(indices.toArray(new String[indices.size()])).setSource(ElasticSearchUtil.parseQuery(queryStringObject.toString()));
		} catch (Exception e) {
			ac.fail(new GenericRestException(BAD_REQUEST, "search_query_not_parsable", e));
			return;
		}
		builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		builder.execute(new ActionListener<SearchResponse>() {

			@Override
			public void onResponse(SearchResponse response) {
				db.tx(() -> {
					List<ObservableFuture<Tuple<T, String>>> obs = new ArrayList<>();
					List<String> requestedLanguageTags = ac.getNodeParameters().getLanguageList();

					for (SearchHit hit : response.getHits()) {

						String id = hit.getId();
						int pos = id.indexOf("-");

						String language = pos > 0 ? id.substring(pos + 1) : null;
						String uuid = pos > 0 ? id.substring(0, pos) : id;

						ObservableFuture<Tuple<T, String>> obsResult = RxHelper.observableFuture();
						obs.add(obsResult);

						// TODO check permissions without loading the vertex

						// Locate the node
						T element = rootVertex.call().findByUuid(uuid);
						if (element == null) {
							log.error("Object could not be found for uuid {" + uuid + "} in root vertex {" + rootVertex.call().getRootLabel() + "}");
							obsResult.toHandler().handle(Future.succeededFuture());
						} else {
							obsResult.toHandler().handle(Future.succeededFuture(Tuple.tuple(element, language)));
						}

					}

					Observable.merge(obs).collect(() -> {
						return new ArrayList<Tuple<T, String>>();
					}, (x, y) -> {
						if (y == null) {
							return;
						}
						boolean matchesRequestedLang = y.v2() == null || requestedLanguageTags == null || requestedLanguageTags.isEmpty()
								|| requestedLanguageTags.contains(y.v2());
						// Check permissions and language
						if (y != null && matchesRequestedLang && requestUser.hasPermission(y.v1(), permission)) {
							x.add(y);
						}
					}).subscribe(list -> {
						// Internally we start with page 0
						int page = pagingInfo.getPage() - 1;

						int low = page * pagingInfo.getPerPage();
						int upper = low + pagingInfo.getPerPage() - 1;

						int n = 0;
						List<Single<TR>> transformedElements = new ArrayList<>();
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

						List<Observable<TR>> obsList = transformedElements.stream().map(ele -> ele.toObservable()).collect(Collectors.toList());
						// Populate the response data with the transformed elements and send the response
						Observable.concat(Observable.from(obsList)).collect(() -> {
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
			}

			@Override
			public void onFailure(Exception e) {
				log.error("Search query failed", e);
				ac.fail(error(BAD_REQUEST, "search_error_query"));
			}
		});

	}

	public void handleStatus(InternalActionContext ac) {
		db.tx(() -> {
			SearchStatusResponse statusResponse = new SearchStatusResponse();
			return Observable.just(statusResponse);
		}).subscribe(message -> ac.send(message, OK), ac::fail);
	}

	public void handleReindex(InternalActionContext ac) {
		db.operateTx(() -> {
			if (ac.getUser().hasAdminRole()) {
				searchProvider.clear();

				// Iterate over all index handlers update the index
				for (IndexHandler<?> handler : registry.getHandlers()) {
					// Create all indices and mappings
					handler.init().await();
					searchProvider.refreshIndex();
					handler.reindexAll().await();
				}
				return Single.just(message(ac, "search_admin_reindex_invoked"));
			} else {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
		}).subscribe(message -> ac.send(message, OK), ac::fail);
	}

	public void createMappings(InternalActionContext ac) {
		utils.operateTx(ac, () -> {
			if (ac.getUser().hasAdminRole()) {
				for (IndexHandler<?> handler : registry.getHandlers()) {
					handler.init().await();
				}
				nodeIndexHandler.updateNodeIndexMappings();
				return message(ac, "search_admin_createmappings_created");
			} else {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
		}, message -> ac.send(message, OK));
	}

}
