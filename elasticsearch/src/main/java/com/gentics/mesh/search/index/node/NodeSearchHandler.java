package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.mapToMeshError;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.elasticsearch.client.okhttp.RequestBuilder;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.SearchClient;
import com.gentics.mesh.search.index.AbstractSearchHandler;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Collection of handlers which are used to deal with search requests.
 */
@Singleton
public class NodeSearchHandler extends AbstractSearchHandler<Node, NodeResponse> {

	private static final Logger log = LoggerFactory.getLogger(NodeSearchHandler.class);

	private BootstrapInitializer boot;

	@Inject
	public NodeSearchHandler(SearchProvider searchProvider, Database db, NodeIndexHandler nodeIndexHandler, HandlerUtilities utils,
		BootstrapInitializer boot) {
		super(db, searchProvider, nodeIndexHandler);
		this.boot = boot;
	}

	/**
	 * Invoke the given query and return a page of node containers.
	 * 
	 * @param gc
	 * @param query
	 *            Elasticsearch query
	 * @param pagingInfo
	 * @return
	 * @throws MeshConfigurationException
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public Page<? extends NodeContent> handleContainerSearch(InternalActionContext ac, String query, PagingParameters pagingInfo,
		GraphPermission... permissions) throws MeshConfigurationException, InterruptedException, ExecutionException, TimeoutException {
		SearchClient client = searchProvider.getClient();
		if (log.isDebugEnabled()) {
			log.debug("Invoking search with query {" + query + "} for {Containers}");
		}
		Set<String> indices = getIndexHandler().getSelectedIndices(ac);

		// Add permission checks to the query
		JsonObject queryJson = prepareSearchQuery(ac, query, true);

		// Apply paging
		applyPagingParams(queryJson, pagingInfo);

		// Only load the documentId we don't care about the indexed contents. The graph is our source of truth here.
		queryJson.put("_source", false);

		if (log.isDebugEnabled()) {
			log.debug("Using parsed query {" + queryJson.encodePrettily() + "}");
		}
		try {
			RequestBuilder<JsonObject> scrollRequest = client.search(queryJson, new ArrayList<>(indices));
			JsonObject scrollResp = scrollRequest.sync();
			JsonObject hitsInfo = scrollResp.getJsonObject("hits");

			// The scrolling iterator will wrap the current response and query ES for more data if needed.
			Page<? extends NodeContent> page = db.tx(() -> {
				long totalCount = hitsInfo.getLong("total");
				List<NodeContent> elementList = new ArrayList<>();
				JsonArray hits = hitsInfo.getJsonArray("hits");
				for (int i = 0; i < hits.size(); i++) {
					JsonObject hit = hits.getJsonObject(i);

					String id = hit.getString("_id");
					int pos = id.indexOf("-");

					String language = pos > 0 ? id.substring(pos + 1) : null;
					String uuid = pos > 0 ? id.substring(0, pos) : id;

					RootVertex<Node> root = getIndexHandler().getRootVertex();
					Node element = root.findByUuid(uuid);
					if (element == null) {
						log.warn("Object could not be found for uuid {" + uuid + "} in root vertex {" + root.getRootLabel() + "}");
						totalCount--;
						continue;
					}

					ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
					Language languageTag = boot.languageRoot().findByLanguageTag(language);
					if (languageTag == null) {
						log.warn("Could not find language {" + language + "}");
						totalCount--;
						continue;
					}

					// Locate the matching container and add it to the list of found containers
					NodeGraphFieldContainer container = element.getGraphFieldContainer(languageTag, ac.getRelease(), type);
					if (container != null) {
						elementList.add(new NodeContent(element, container));
					} else {
						totalCount--;
						continue;
					}

				}
				// Update the total count
				hitsInfo.put("total", totalCount);

				PagingMetaInfo info = extractMetaInfo(hitsInfo, pagingInfo);
				return new PageImpl<>(elementList, info.getTotalCount(), pagingInfo.getPage(), info.getPageCount(), pagingInfo.getPerPage());
			});
			return page;
		} catch (HttpErrorException e) {
			log.error("Error while processing query", e);
			throw mapToMeshError(e);
		}

	}

}
