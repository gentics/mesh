package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.mapError;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.mapToMeshError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.elasticsearch.client.okhttp.RequestBuilder;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.action.NodeDAOActions;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Collection of handlers which are used to deal with search requests.
 */
@Singleton
public class NodeSearchHandler extends AbstractSearchHandler<HibNode, NodeResponse> {

	private static final Logger log = LoggerFactory.getLogger(NodeSearchHandler.class);

	@Inject
	public NodeSearchHandler(SearchProvider searchProvider, Database db, NodeIndexHandlerImpl nodeIndexHandler,
		MeshOptions options, NodeDAOActions actions) {
		super(db, searchProvider, options, nodeIndexHandler, actions);
	}

	/**
	 * Invoke the given query and return a page of node containers.
	 * 
	 * @param ac
	 * @param query
	 *            Elasticsearch query
	 * @param pagingInfo
	 * @param type
	 * @return
	 * @throws MeshConfigurationException
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public Page<? extends NodeContent> handleContainerSearch(InternalActionContext ac, String query, PagingParameters pagingInfo, ContainerType type,
		InternalPermission... permissions) throws MeshConfigurationException, InterruptedException, ExecutionException, TimeoutException {
		ElasticsearchClient<JsonObject> client = searchProvider.getClient();
		if (client == null) {
			throw error(HttpResponseStatus.SERVICE_UNAVAILABLE, "search_error_elasticsearch_not_available");
		}
		if (log.isDebugEnabled()) {
			log.debug("Invoking search with query {" + query + "} for {Containers}");
		}
		Set<String> indices = getIndexHandler().getIndicesForSearch(ac, type);

		// Add permission checks to the query
		JsonObject queryJson = prepareSearchQuery(ac, query, true);

		// Apply paging
		applyPagingParams(queryJson, pagingInfo);

		// Only load the documentId we don't care about the indexed contents. The graph is our source of truth here.
		queryJson.put("_source", false);

		if (log.isDebugEnabled()) {
			log.debug("Using parsed query {" + queryJson.encodePrettily() + "}");
		}

		JsonObject queryOption = new JsonObject();
		queryOption.put("index", StringUtils.join(indices.stream().map(i -> searchProvider.installationPrefix() + i).toArray(String[]::new), ","));
		queryOption.put("search_type", "dfs_query_then_fetch");
		log.debug("Using options {" + queryOption.encodePrettily() + "}");

		try {
			RequestBuilder<JsonObject> searchRequest = client.multiSearch(queryOption, queryJson);
			JsonObject response = searchRequest.sync();
			JsonArray responses = response.getJsonArray("responses");
			JsonObject firstResponse = responses.getJsonObject(0);

			// Process the nested error
			JsonObject errorInfo = firstResponse.getJsonObject("error");
			if (errorInfo != null) {
				throw mapError(errorInfo);
			}

			JsonObject hitsInfo = firstResponse.getJsonObject("hits");

			// The scrolling iterator will wrap the current response and query ES for more data if needed.
			Page<? extends NodeContent> page = db.tx(tx -> {
				ContentDao contentDao = tx.contentDao();
				long totalCount = extractTotalCount(hitsInfo);
				List<NodeContent> elementList = new ArrayList<>();
				JsonArray hits = hitsInfo.getJsonArray("hits");
				for (int i = 0; i < hits.size(); i++) {
					JsonObject hit = hits.getJsonObject(i);

					String id = hit.getString("_id");
					int pos = id.indexOf("-");

					String languageTag = pos > 0 ? id.substring(pos + 1) : null;
					String uuid = pos > 0 ? id.substring(0, pos) : id;

					HibNode element = getIndexHandler().elementLoader().apply(uuid);
					if (element == null) {
						log.warn("Object could not be found for uuid {" + uuid + "}");
						totalCount--;
						continue;
					}

					HibLanguage language = tx.languageDao().findByLanguageTag(languageTag);
					if (language == null) {
						log.warn("Could not find language {" + languageTag + "}");
						totalCount--;
						continue;
					}

					// Locate the matching container and add it to the list of found containers
					HibNodeFieldContainer container = contentDao.getGraphFieldContainer(element, languageTag, tx.getBranch(ac), type);
					if (container != null) {
						elementList.add(new NodeContent(element, container, Arrays.asList(languageTag), type));
					} else {
						totalCount--;
						continue;
					}

				}
				// Update the total count
				switch (complianceMode) {
				case ES_6:
					hitsInfo.put("total", totalCount);
					break;
				case ES_7:
					hitsInfo.put("total", new JsonObject().put("value", totalCount));
					break;
				default:
					throw new RuntimeException("Unknown compliance mode {" + complianceMode + "}");

				}

				PagingMetaInfo info = extractMetaInfo(hitsInfo, pagingInfo);
				return new PageImpl<>(elementList, info.getTotalCount(), pagingInfo.getPage(), info.getPageCount(), pagingInfo.getPerPage());
			});
			return page;
		} catch (HttpErrorException e) {
			log.error("Error while processing query", e);
			throw mapToMeshError(e);
		}

	}

	@Override
	public NodeIndexHandlerImpl getIndexHandler() {
		return (NodeIndexHandlerImpl) super.getIndexHandler();
	}

}
