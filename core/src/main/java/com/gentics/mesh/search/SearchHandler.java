package com.gentics.mesh.search;

import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.VerticleHelper.fail;
import static com.gentics.mesh.util.VerticleHelper.getPagingInfo;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.send;

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

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

@Component
public class SearchHandler {

	private static final Logger log = LoggerFactory.getLogger(SearchHandler.class);

	@Autowired
	private ElasticSearchProvider elasticSearchProvider;

	@Autowired
	private Database db;

	public <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void handleSearch(RoutingContext rc,
			RootVertex<T> rootVertex, Class<RL> classOfRL)
					throws InstantiationException, IllegalAccessException, InvalidArgumentException, MeshJsonException {

		PagingInfo pagingInfo = getPagingInfo(rc);
		if (pagingInfo.getPage() < 1) {
			throw new InvalidArgumentException("The page must always be positive");
		}
		if (pagingInfo.getPerPage() < 1) {
			throw new InvalidArgumentException("The pageSize must always be positive");
		}

		RL listResponse = classOfRL.newInstance();
		MeshAuthUser requestUser = getUser(rc);
		Client client = elasticSearchProvider.getNode().client();

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
			throw new MeshJsonException("Could not parse query string {" + searchQuery + "}", e);
		}
		builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		builder.execute().addListener(new ActionListener<SearchResponse>() {

			@Override
			public void onResponse(SearchResponse response) {
				try (Trx tx = db.trx()) {
					List<T> elements = new ArrayList<>();
					for (SearchHit hit : response.getHits()) {
						String uuid = hit.getId();

						// Locate the node
						rootVertex.findByUuid(uuid, rh -> {
							if (rh.result() != null && rh.succeeded()) {
								T element = rh.result();
								/* Check permissions */
								if (requestUser.hasPermission(element, GraphPermission.READ_PERM)) {
									elements.add(element);
								}
							} else {
								log.error("Could not find node {" + uuid + "}", rh.cause());
							}
						});

					}

					// Internally we start with page 0
					int page = pagingInfo.getPage() - 1;

					int low = page * pagingInfo.getPerPage();
					int upper = low + pagingInfo.getPerPage() - 1;

					int n = 0;
					for (T element : elements) {
						//Only transform elements that we want to list in our resultset
						if (n >= low && n <= upper) {
							// Transform node and add it to the list of nodes
							element.transformToRest(rc, th -> {
								listResponse.getData().add(th.result());
							});
						}
						n++;
					}

					PagingMetaInfo metainfo = new PagingMetaInfo();
					int totalPages = (int) Math.ceil(elements.size() / (double) pagingInfo.getPerPage());
					// Cap totalpages to 1
					if (totalPages == 0) {
						totalPages = 1;
					}
					metainfo.setTotalCount(elements.size());

					metainfo.setCurrentPage(pagingInfo.getPage());
					metainfo.setPageCount(totalPages);
					metainfo.setPerPage(pagingInfo.getPerPage());
					listResponse.setMetainfo(metainfo);

					send(rc, toJson(listResponse));
				}
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Search query failed", e);
				fail(rc, "search_error_query");
			}
		});

	}
}
