package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.MeshSearchHit;
import com.gentics.mesh.search.ScrollingIterator;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.SearchClient;
import com.gentics.mesh.search.index.AbstractSearchHandler;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Collection of handlers which are used to deal with search requests.
 */
@Singleton
public class NodeSearchHandler extends AbstractSearchHandler<Node, NodeResponse> {

	private static final Logger log = LoggerFactory.getLogger(NodeSearchHandler.class);

	private static final int INITIAL_BATCH_SIZE = 30;

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
		JsonObject queryJson = prepareSearchQuery(ac, query);
		if (log.isDebugEnabled()) {
			log.debug("Using parsed query {" + queryJson.encodePrettily() + "}");
		}
		// Only load the documentId we don't care about the indexed contents. The graph is our source of truth here.
		queryJson.put("source", false);
		queryJson.put("size", INITIAL_BATCH_SIZE);
		queryJson.put("scroll", "1m");

		try {
			JsonObject scrollResp = client.queryScroll(query, indices.toArray(new String[indices.size()]));
			long unfilteredCount = scrollResp.getJsonObject("hits").getLong("totalHits");
			// The scrolling iterator will wrap the current response and query ES for more data if needed.
			ScrollingIterator scrollingIt = new ScrollingIterator(client, scrollResp);
			Page<? extends NodeContent> page = db.tx(() -> {

				// Prepare a stream which applies all needed filtering
				Stream<NodeContent> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(scrollingIt, Spliterator.ORDERED), false)

						.map(hit -> {
							String id = hit.getString("id");
							int pos = id.indexOf("-");

							String language = pos > 0 ? id.substring(pos + 1) : null;
							String uuid = pos > 0 ? id.substring(0, pos) : id;

							return new MeshSearchHit<Node>(uuid, language);
						})
						// TODO filter by requested language
						.filter(hit -> {
							return hit.language != null;
						})

						.map(hit -> {
							// Load the node
							RootVertex<Node> root = getIndexHandler().getRootVertex();
							hit.element = root.findByUuid(hit.uuid);
							if (hit.element == null) {
								log.error("Object could not be found for uuid {" + hit.uuid + "} in root vertex {" + root.getRootLabel() + "}");
							}

							return hit;
						})

						.filter(hit -> {
							// Only include found elements
							return hit.element != null;
						})

						.map(hit -> {

							ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
							Language languageTag = boot.languageRoot().findByLanguageTag(hit.language);
							if (languageTag == null) {
								log.debug("Could not find language {" + hit.language + "}");
								return null;
							}

							// Locate the matching container and add it to the list of found containers
							NodeGraphFieldContainer container = hit.element.getGraphFieldContainer(languageTag, ac.getRelease(), type);
							if (container != null) {
								return new NodeContent(hit.element, container);
							}
							return null;
						})

						.filter(hit -> {
							return hit != null;
						});
				DynamicStreamPageImpl<NodeContent> dynamicPage = new DynamicStreamPageImpl<>(stream, pagingInfo);
				dynamicPage.setUnfilteredSearchCount(unfilteredCount);
				return dynamicPage;
			});
			return page;
		} catch (IOException e) {
			// throw error(BAD_REQUEST, "search_query_not_parsable", e);

			log.error("Error while processing query", e);
			throw error(BAD_REQUEST, "search_error_query", e);
		}

	}

}
