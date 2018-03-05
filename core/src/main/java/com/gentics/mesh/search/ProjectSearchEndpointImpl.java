package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.POST;

import java.util.function.Supplier;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;
import com.gentics.mesh.search.index.node.NodeSearchHandler;
import com.gentics.mesh.search.index.tag.TagSearchHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilySearchHandler;

/**
 * Verticle that adds REST endpoints for project specific search (for nodes, tags and tagFamilies)
 */
public class ProjectSearchEndpointImpl extends AbstractProjectEndpoint implements SearchEndpoint {

	@Inject
	public NodeSearchHandler nodeSearchHandler;

	@Inject
	public TagSearchHandler tagSearchHandler;

	@Inject
	public TagFamilySearchHandler tagFamilySearchHandler;

	public ProjectSearchEndpointImpl() {
		super("search", null);
	}

	@Inject
	public ProjectSearchEndpointImpl(BootstrapInitializer boot) {
		super("search", boot);
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
		registerSearchHandler("nodes", () -> boot.meshRoot().getNodeRoot(), NodeListResponse.class, nodeSearchHandler, nodeExamples
			.getNodeListResponse(), true);
		registerSearchHandler("tags", () -> boot.meshRoot().getTagRoot(), TagListResponse.class, tagSearchHandler, tagExamples
			.createTagListResponse(), false);
		registerSearchHandler("tagFamilies", () -> boot.meshRoot().getTagFamilyRoot(), TagFamilyListResponse.class, tagFamilySearchHandler,
			tagFamilyExamples.getTagFamilyListResponse(), false);

	}

	/**
	 * Register the search handler which will parse the search result and return a mesh list response.
	 * 
	 * @param typeName
	 *            Name of the search endpoint
	 * @param root
	 *            Aggregation node that should be used to load the objects that were found within the search index
	 * @param classOfRL
	 *            Class of matching list response
	 * @param indexHandlerKey
	 *            index handler key
	 * @param exampleResponse
	 *            Example list response used for RAML generation
	 * @param filterByLanguage
	 *            Whether to append the language filter
	 */
	private <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void registerSearchHandler(String typeName,
		Supplier<RootVertex<T>> root, Class<RL> classOfRL, SearchHandler<T, TR> searchHandler, RL exampleResponse, boolean filterByLanguage) {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/" + typeName);
		endpoint.method(POST);
		endpoint.description("Invoke a search query for " + typeName + " and return a paged list response.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, exampleResponse, "Paged search result list.");
		endpoint.exampleRequest(miscExamples.getSearchQueryExample());
		endpoint.handler(rc -> {
			try {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				searchHandler.query(ac, root, classOfRL, filterByLanguage);
			} catch (Exception e) {
				rc.fail(e);
			}
		});
	}
}
