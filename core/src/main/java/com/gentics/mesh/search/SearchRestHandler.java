package com.gentics.mesh.search;

import static com.gentics.mesh.json.JsonUtil.toJson;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.ArrayList;
import java.util.HashSet;
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

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
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

	public <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void handleSearch(RoutingContext rc,
			RootVertex<T> rootVertex, Class<RL> classOfRL)
					throws InstantiationException, IllegalAccessException, InvalidArgumentException, MeshJsonException {

		ActionContext ac = ActionContext.create(rc);
		PagingInfo pagingInfo = ac.getPagingInfo();
		if (pagingInfo.getPage() < 1) {
			throw new InvalidArgumentException("The page must always be positive");
		}
		if (pagingInfo.getPerPage() < 1) {
			throw new InvalidArgumentException("The pageSize must always be positive");
		}

		RL listResponse = classOfRL.newInstance();
		MeshAuthUser requestUser = ac.getUser();
		Client client = searchProvider.getNode().client();

		String searchQuery = rc.getBodyAsString();
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
			builder = client.prepareSearch().setSource(searchQuery);
		} catch (Exception e) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("search_query_not_parsable"), e));
			return;
		}
		builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		builder.execute().addListener(new ActionListener<SearchResponse>() {

			@Override
			public void onResponse(SearchResponse response) {
				try (Trx tx = db.trx()) {
					Set<ObservableFuture<T>> futures = new HashSet<>();

					for (SearchHit hit : response.getHits()) {
						String uuid = hit.getId();
						ObservableFuture<T> obs = RxHelper.observableFuture();
						futures.add(obs);

						// Locate the node
						rootVertex.findByUuid(uuid, rh -> {
							if (rh.failed()) {
								obs.toHandler().handle(Future.failedFuture(rh.cause()));
							} else if (rh.result() == null) {
								obs.toHandler().handle(
										Future.failedFuture(new HttpStatusCodeErrorException(NOT_FOUND, ac.i18n("object_not_found_for_uuid", uuid))));
							} else {
								T element = rh.result();
								obs.toHandler().handle(Future.succeededFuture(element));
							}
						});
					}
					Observable<T> merged = Observable.merge(futures);
					merged.collect(() -> {
						return new ArrayList<T>();
					} , (x, y) -> {
						// Check permissions
						if (requestUser.hasPermission(y, GraphPermission.READ_PERM)) {
							x.add(y);
						}
					}).subscribe(list -> {
						// Internally we start with page 0
						int page = pagingInfo.getPage() - 1;

						int low = page * pagingInfo.getPerPage();
						int upper = low + pagingInfo.getPerPage() - 1;

						int n = 0;
						for (T element : list) {
							//Only transform elements that we want to list in our resultset
							if (n >= low && n <= upper) {
								// Transform node and add it to the list of nodes
								element.transformToRest(ac, th -> {
									listResponse.getData().add(th.result());
								});
							}
							n++;
						}

						PagingMetaInfo metainfo = new PagingMetaInfo();
						int totalPages = (int) Math.ceil(list.size() / (double) pagingInfo.getPerPage());
						// Cap totalpages to 1
						if (totalPages == 0) {
							totalPages = 1;
						}
						metainfo.setTotalCount(list.size());

						metainfo.setCurrentPage(pagingInfo.getPage());
						metainfo.setPageCount(totalPages);
						metainfo.setPerPage(pagingInfo.getPerPage());
						listResponse.setMetainfo(metainfo);

						ac.send(toJson(listResponse));
					});
					merged.subscribe(item -> {
						log.debug("Loaded node {" + item.getUuid() + "}");
					} , error -> {
						log.error("Error while processing search response items", error);
						rc.fail(error);
					});
				}
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Search query failed", e);
				ac.fail(BAD_REQUEST, "search_error_query");
			}
		});

	}
}
