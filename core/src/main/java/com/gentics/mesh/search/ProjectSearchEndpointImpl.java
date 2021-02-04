package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.POST;

import java.util.function.Function;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;
import com.gentics.mesh.search.index.node.NodeSearchHandler;
import com.gentics.mesh.search.index.tag.TagSearchHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilySearchHandler;

/**
 * Verticle that adds REST endpoints for project specific search (for nodes, tags and tagFamilies)
 */
public class ProjectSearchEndpointImpl extends AbstractProjectEndpoint implements SearchEndpoint {

	public final Database db;

	public final NodeSearchHandler nodeSearchHandler;

	public final TagSearchHandler tagSearchHandler;

	public final TagFamilySearchHandler tagFamilySearchHandler;

	public ProjectSearchEndpointImpl() {
		super("search", null, null);
		this.db = null;
		this.nodeSearchHandler = null;
		this.tagSearchHandler = null;
		this.tagFamilySearchHandler = null;
	}

	@Inject
	public ProjectSearchEndpointImpl(MeshAuthChainImpl chain, BootstrapInitializer boot, Database db, TagFamilySearchHandler tagFamilySearchHandler,
		TagSearchHandler tagSearchHandler, NodeSearchHandler nodeSearchHandler) {
		super("search", chain, boot);
		this.db = db;
		this.nodeSearchHandler = nodeSearchHandler;
		this.tagSearchHandler = tagSearchHandler;
		this.tagFamilySearchHandler = tagFamilySearchHandler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow project wide search.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addSearchEndpoints();
	}

	/**
	 * Add various search endpoints using the aggregation nodes.
	 */
	private void addSearchEndpoints() {
		registerSearchHandler("nodes", (uuid) -> {
			return db.index().findByUuid(NodeImpl.class, uuid);
		}, NodeListResponse.class, nodeSearchHandler, nodeExamples.getNodeListResponse(), true);

		registerSearchHandler("tags", uuid -> {
			HibTag tag = Tx.get().tagDao().findByUuidGlobal(uuid);
			return tag;
		}, TagListResponse.class, tagSearchHandler, tagExamples.createTagListResponse(), false);

		registerSearchHandler("tagFamilies", uuid -> {
			TagFamilyDao tagFamilyDao = Tx.get().tagFamilyDao();
			return tagFamilyDao.findByUuid(uuid);
		}, TagFamilyListResponse.class, tagFamilySearchHandler, tagFamilyExamples.getTagFamilyListResponse(), false);
	}

	/**
	 * Register the search handler which will parse the search result and return a mesh list response.
	 * 
	 * @param typeName
	 *            Name of the search endpoint
	 * @param elementLoader
	 *            Loader function which will load the element with the given uuid from the graph.
	 * @param classOfRL
	 *            Class of matching list response
	 * @param indexHandlerKey
	 *            index handler key
	 * @param exampleResponse
	 *            Example list response used for RAML generation
	 * @param filterByLanguage
	 *            Whether to append the language filter
	 */
	private <T extends HibCoreElement, TR extends RestModel, RL extends ListResponse<TR>> void registerSearchHandler(String typeName,
		Function<String, T> elementLoader, Class<RL> classOfRL, SearchHandler<T, TR> searchHandler, RL exampleResponse, boolean filterByLanguage) {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/" + typeName);
		endpoint.method(POST);
		endpoint.description("Invoke a search query for " + typeName + " and return a paged list response.");
		endpoint.setMutating(false);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, exampleResponse, "Paged search result list.");
		endpoint.exampleRequest(miscExamples.getSearchQueryExample());
		endpoint.handler(rc -> {
			try {
				InternalActionContext ac = wrap(rc);
				searchHandler.query(ac, elementLoader, classOfRL, filterByLanguage);
			} catch (Exception e) {
				rc.fail(e);
			}
		});
	}
}
