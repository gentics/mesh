package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.search.index.IndexHandler;

import rx.functions.Func0;

/**
 * Verticle that adds REST endpoints for project specific search (for nodes, tags and tagFamilies)
 */
@Singleton
public class ProjectSearchVerticle extends AbstractProjectRestVerticle {

	private SearchRestHandler searchHandler;

	private IndexHandlerRegistry registry;

	public ProjectSearchVerticle() {
		super("search", null, null);
	}

	@Inject
	public ProjectSearchVerticle(BootstrapInitializer boot, RouterStorage routerStorage, SearchRestHandler searchHandler,
			IndexHandlerRegistry registry) {
		super("search", boot, routerStorage);
		this.searchHandler = searchHandler;
		this.registry = registry;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow project wide search.";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		addSearchEndpoints();
	}

	/**
	 * Add various search endpoints using the aggregation nodes.
	 */
	private void addSearchEndpoints() {
		registerSearchHandler("nodes", () -> boot.meshRoot().getNodeRoot(), NodeListResponse.class, Node.TYPE, nodeExamples.getNodeListResponse());
		registerSearchHandler("tags", () -> boot.meshRoot().getTagRoot(), TagListResponse.class, Tag.TYPE, tagExamples.getTagListResponse());
		registerSearchHandler("tagFamilies", () -> boot.meshRoot().getTagFamilyRoot(), TagFamilyListResponse.class, TagFamily.TYPE,
				tagFamilyExamples.getTagFamilyListResponse());
	}

	/**
	 * Register the selected search handler.
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
	 */
	private <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void registerSearchHandler(String typeName,
			Func0<RootVertex<T>> root, Class<RL> classOfRL, String indexHandlerKey, RL exampleResponse) {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/" + typeName);
		endpoint.method(POST);
		endpoint.description("Invoke a search query for " + typeName + " and return a paged list response.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, exampleResponse, "Paged search result list.");
		endpoint.exampleRequest(miscExamples.getSearchQueryExample());
		endpoint.handler(rc -> {
			try {
				IndexHandler indexHandler = registry.get(indexHandlerKey);
				InternalActionContext ac = InternalActionContext.create(rc);
				searchHandler.handleSearch(ac, root, classOfRL, indexHandler.getSelectedIndices(ac), indexHandler.getReadPermission(ac));
			} catch (Exception e) {
				rc.fail(e);
			}
		});
	}
}
