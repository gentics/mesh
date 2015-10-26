package com.gentics.mesh.search;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RxUtil;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

@Component
public class SearchRestHandler {

	private static final Logger log = LoggerFactory.getLogger(SearchRestHandler.class);

	@Autowired
	private SearchProvider searchProvider;

	@Autowired
	private Database db;

	/**
	 * Handle a search request.
	 * 
	 * @param ac
	 * @param rootVertex
	 *            Root Vertex of the elements that should be searched
	 * @param classOfRL
	 *            Class of the rest model list that should be used when creating the response
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvalidArgumentException
	 * @throws MeshJsonException
	 */
	public <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void handleSearch(InternalActionContext ac,
			RootVertex<T> rootVertex, Class<RL> classOfRL)
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
			queryStringObject.put("from", 0);
			queryStringObject.put("size", Integer.MAX_VALUE);
			builder = client.prepareSearch().setSource(queryStringObject.toString());
		} catch (Exception e) {
			ac.fail(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("search_query_not_parsable"), e));
			return;
		}
		builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		builder.execute().addListener(new ActionListener<SearchResponse>() {

			@Override
			public void onResponse(SearchResponse response) {
				db.noTrx(noTrx -> {
					rootVertex.reload();

					List<ObservableFuture<T>> futures = new ArrayList<>();
					for (SearchHit hit : response.getHits()) {
						String uuid = hit.getId();
						ObservableFuture<T> obs = RxHelper.observableFuture();
						futures.add(obs);

						// Locate the node
						rootVertex.findByUuid(uuid, rh -> {
							if (rh.failed()) {
								obs.toHandler().handle(Future.failedFuture(rh.cause()));
							} else if (rh.result() == null) {
								log.error("Object could not be found for uuid {" + uuid + "} in root vertex {" + rootVertex.getImpl().getFermaType()
										+ "}");
								obs.toHandler().handle(Future.succeededFuture());
							} else {
								T element = rh.result();
								obs.toHandler().handle(Future.succeededFuture(element));
							}
						});
					}
					Observable.merge(futures).collect(() -> {
						return new ArrayList<T>();
					} , (x, y) -> {
						// Check permissions
						if (y != null && requestUser.hasPermission(ac, y, GraphPermission.READ_PERM)) {
							x.add(y);
						}
					}).subscribe(list -> {
						// Internally we start with page 0
						int page = pagingInfo.getPage() - 1;

						int low = page * pagingInfo.getPerPage();
						int upper = low + pagingInfo.getPerPage() - 1;

						int n = 0;
						List<ObservableFuture<TR>> transformedElements = new ArrayList<>();
						for (T element : list) {
							// Only transform elements that we want to list in our resultset
							if (n >= low && n <= upper) {
								ObservableFuture<TR> obs = RxHelper.observableFuture();
								transformedElements.add(obs);
								// Transform node and add it to the list of nodes
								element.transformToRest(ac, obs.toHandler());
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

				});
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Search query failed", e);
				ac.fail(BAD_REQUEST, "search_error_query");
			}
		});

	}

}
