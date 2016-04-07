package com.gentics.mesh.search;

import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.search.index.IndexHandler;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RxUtil;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

/**
 * Collection of handlers which are used to deal with rest search requests.
 */
@Component
public class SearchRestHandler {

	private static final Logger log = LoggerFactory.getLogger(SearchRestHandler.class);

	@Autowired
	private BootstrapInitializer boot;

	@Autowired
	private SearchProvider searchProvider;

	@Autowired
	private Database db;

	@Autowired
	private IndexHandlerRegistry registry;

	/**
	 * Handle a search request.
	 * 
	 * @param ac
	 * @param rootVertex
	 *            Root Vertex of the elements that should be searched
	 * @param classOfRL
	 *            Class of the rest model list that should be used when creating the response
	 * @param indices index names to search
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvalidArgumentException
	 * @throws MeshJsonException
	 */
	public <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void handleSearch(InternalActionContext ac,
			RootVertex<T> rootVertex, Class<RL> classOfRL, Set<String> indices)
					throws InstantiationException, IllegalAccessException, InvalidArgumentException, MeshJsonException {

		PagingParameter pagingInfo = ac.getPagingParameter();
		if (pagingInfo.getPage() < 1) {
			throw new InvalidArgumentException("The page must always be positive");
		}
		if (pagingInfo.getPerPage() < 0) {
			throw new InvalidArgumentException("The pageSize must always be zero or greater than zero");
		}

		RL listResponse = classOfRL.newInstance();
		MeshAuthUser requestUser = ac.getUser();
		Client client = searchProvider.getNode().client();

		String searchQuery = ac.getBodyAsString();
		if (log.isDebugEnabled()) {
			log.debug("Invoking search with query {" + searchQuery + "} for {" + classOfRL.getName() + "}");
		}

		/*
		 * TODO, FIXME This a very crude hack but we need to handle paging ourself for now. In order to avoid such nasty ways of paging a custom ES plugin has
		 * to be written that deals with Document Level Permissions/Security (common known as DLS)
		 */
		SearchRequestBuilder builder = null;
		try {
			JSONObject queryStringObject = new JSONObject(searchQuery);
			/**
			 * Note that from + size can not be more than the index.max_result_window index setting which defaults to 10,000. See the Scroll API for more efficient ways to do deep scrolling.
			 */
			queryStringObject.put("from", 0);
			queryStringObject.put("size", Integer.MAX_VALUE);
			builder = client.prepareSearch(indices.toArray(new String[indices.size()]))
					.setSource(queryStringObject.toString());
		} catch (Exception e) {
			ac.fail(new HttpStatusCodeErrorException(BAD_REQUEST, "search_query_not_parsable", e));
			return;
		}
		builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		builder.execute().addListener(new ActionListener<SearchResponse>() {

			@Override
			public void onResponse(SearchResponse response) {
				db.noTrx(() -> {
					rootVertex.reload();

					List<ObservableFuture<Tuple<T, String>>> obs = new ArrayList<>();
					List<String> requestedLanguageTags = ac.getSelectedLanguageTags();

					for (SearchHit hit : response.getHits()) {

						String id = hit.getId();
						int pos = id.indexOf("-");

						String language = pos > 0 ? id.substring(pos + 1) : null;
						String uuid = pos > 0 ? id.substring(0, pos) : id;

						ObservableFuture<Tuple<T, String>> obsResult = RxHelper.observableFuture();
						obs.add(obsResult);

						// Locate the node
						rootVertex.findByUuid(uuid).subscribe(element -> {
							if (element == null) {
								log.error("Object could not be found for uuid {" + uuid + "} in root vertex {" + rootVertex.getImpl().getFermaType()
										+ "}");
								obsResult.toHandler().handle(Future.succeededFuture());
							} else {
								obsResult.toHandler().handle(Future.succeededFuture(Tuple.tuple(element, language)));
							}
						} , error -> {
							obsResult.toHandler().handle(Future.failedFuture(error));
						});
					}

					Observable.merge(obs).collect(() -> {
						return new ArrayList<Tuple<T, String>>();
					} , (x, y) -> {
						// Check permissions and language
						if (y != null && requestUser.hasPermissionSync(ac, y.v1(), GraphPermission.READ_PERM)
								&& (y.v2() == null || requestedLanguageTags.contains(y.v2()))) {
							x.add(y);
						}
					}).subscribe(list -> {
						// Internally we start with page 0
						int page = pagingInfo.getPage() - 1;

						int low = page * pagingInfo.getPerPage();
						int upper = low + pagingInfo.getPerPage() - 1;

						int n = 0;
						List<Observable<TR>> transformedElements = new ArrayList<>();
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
						int totalPages = (int) Math.ceil(list.size() / (double) pagingInfo.getPerPage());
						// Cap totalpages to 1
						totalPages = totalPages == 0 ? 1 : totalPages;
						metainfo.setTotalCount(list.size());
						metainfo.setCurrentPage(pagingInfo.getPage());
						metainfo.setPageCount(totalPages);
						metainfo.setPerPage(pagingInfo.getPerPage());
						listResponse.setMetainfo(metainfo);

						// Populate the response data with the transformed elements and send the response
						RxUtil.concatList(transformedElements).collect(() -> {
							return listResponse.getData();
						} , (x, y) -> {
							x.add(y);
						}).subscribe(itemList -> {
							ac.send(JsonUtil.toJson(listResponse), OK);
						} , error -> {
							ac.fail(error);
						});

					} , error -> {
						log.error("Error while processing search response items", error);
						ac.fail(error);
					});
					return null;
				});
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Search query failed", e);
				throw error(BAD_REQUEST, "search_error_query");
			}
		});

	}

	public void handleStatus(InternalActionContext ac) {
		db.noTrx(() -> {
			SearchQueue queue = MeshRoot.getInstance().getSearchQueue();
			SearchStatusResponse statusResponse = new SearchStatusResponse();
			statusResponse.setBatchCount(queue.getSize());
			return Observable.just(statusResponse);
		}).subscribe(message -> ac.respond(message, OK), ac::fail);
	}

	public void handleReindex(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			if (ac.getUser().hasAdminRole()) {
				for (IndexHandler handler : registry.getHandlers()) {
					handler.clearIndex().toBlocking().single();
				}
				boot.meshRoot().getSearchQueue().addFullIndex();
				boot.meshRoot().getSearchQueue().processAll();
				return Observable.just(message(ac, "search_admin_reindex_invoked"));
			} else {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
		}).subscribe(message -> ac.respond(message, OK), ac::fail);

	}

}
