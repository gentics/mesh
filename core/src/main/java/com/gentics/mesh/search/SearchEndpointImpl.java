package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.EndpointRoute;
import com.gentics.mesh.search.index.AdminIndexHandler;
import com.gentics.mesh.search.index.group.GroupSearchHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaSearchHandler;
import com.gentics.mesh.search.index.node.NodeSearchHandler;
import com.gentics.mesh.search.index.project.ProjectSearchHandler;
import com.gentics.mesh.search.index.role.RoleSearchHandler;
import com.gentics.mesh.search.index.schema.SchemaSearchHandler;
import com.gentics.mesh.search.index.tag.TagSearchHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilySearchHandler;
import com.gentics.mesh.search.index.user.UserSearchHandler;

import dagger.Lazy;
import rx.functions.Func0;

@Singleton
public class SearchEndpointImpl extends AbstractEndpoint implements SearchEndpoint {

	private Lazy<BootstrapInitializer> boot;

	@Inject
	AdminIndexHandler adminHandler;

	@Inject
	UserSearchHandler userSearchHandler;

	@Inject
	GroupSearchHandler groupSearchHandler;

	@Inject
	RoleSearchHandler roleSearchHandler;

	@Inject
	NodeSearchHandler nodeSearchHandler;

	@Inject
	TagSearchHandler tagSearchHandler;

	@Inject
	TagFamilySearchHandler tagFamilySearchHandler;

	@Inject
	ProjectSearchHandler projectSearchHandler;

	@Inject
	SchemaSearchHandler schemaContainerSearchHandler;

	@Inject
	MicroschemaSearchHandler microschemaContainerSearchHandler;

	@Inject
	public SearchEndpointImpl(RouterStorage routerStorage, NodeSearchHandler searchHandler, Lazy<BootstrapInitializer> boot) {
		super("search", routerStorage);
		this.boot = boot;
	}

	public SearchEndpointImpl() {
		super("search", null);
	}

	@Override
	public String getDescription() {
		return "Provides search endpoints which can be used to invoke global searches";
	}

	@Override
	public void registerEndPoints() {
		secureAll();
		withBodyHandler();

		addSearchEndpoints();
	}

	/**
	 * Add various search endpoints using the aggregation nodes.
	 */
	private void addSearchEndpoints() {
		registerHandler("users", () -> boot.get().meshRoot().getUserRoot(), UserListResponse.class, userSearchHandler,
				userExamples.getUserListResponse());
		registerHandler("groups", () -> boot.get().meshRoot().getGroupRoot(), GroupListResponse.class, groupSearchHandler,
				groupExamples.getGroupListResponse());
		registerHandler("roles", () -> boot.get().meshRoot().getRoleRoot(), RoleListResponse.class, roleSearchHandler,
				roleExamples.getRoleListResponse());

		registerHandler("nodes", () -> boot.get().meshRoot().getNodeRoot(), NodeListResponse.class, nodeSearchHandler,
				nodeExamples.getNodeListResponse());
		registerHandler("tags", () -> boot.get().meshRoot().getTagRoot(), TagListResponse.class, tagSearchHandler,
				tagExamples.createTagListResponse());
		registerHandler("tagFamilies", () -> boot.get().meshRoot().getTagFamilyRoot(), TagFamilyListResponse.class, tagFamilySearchHandler,
				tagFamilyExamples.getTagFamilyListResponse());

		registerHandler("projects", () -> boot.get().meshRoot().getProjectRoot(), ProjectListResponse.class, projectSearchHandler,
				projectExamples.getProjectListResponse());
		registerHandler("schemas", () -> boot.get().meshRoot().getSchemaContainerRoot(), SchemaListResponse.class, schemaContainerSearchHandler,
				schemaExamples.getSchemaListResponse());
		registerHandler("microschemas", () -> boot.get().meshRoot().getMicroschemaContainerRoot(), MicroschemaListResponse.class,
				microschemaContainerSearchHandler, microschemaExamples.getMicroschemaListResponse());
		addAdminHandlers();
	}

	private void addAdminHandlers() {
		EndpointRoute statusEndpoint = createEndpoint();
		statusEndpoint.path("/status");
		statusEndpoint.method(GET);
		statusEndpoint.description("Returns the search index status.");
		statusEndpoint.produces(APPLICATION_JSON);
		statusEndpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Search index status.");
		statusEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			adminHandler.handleStatus(ac);
		});

		// Endpoint createMappings = createEndpoint();
		// createMappings.path("/createMappings");
		// createMappings.method(POST);
		// createMappings.produces(APPLICATION_JSON);
		// createMappings.description("Create search index mappings.");
		// createMappings.exampleResponse(OK, miscExamples.createMessageResponse(), "Create all mappings.");
		// createMappings.handler(rc -> {
		// InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		// adminHandler.createMappings(ac);
		// });

		EndpointRoute reindexEndpoint = createEndpoint();
		reindexEndpoint.path("/reindex");
		reindexEndpoint.method(POST);
		reindexEndpoint.produces(APPLICATION_JSON);
		reindexEndpoint.description("Invokes a full reindex of the search indices. This operation may take some time to complete.");
		reindexEndpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Invoked reindex command for all elements.");
		reindexEndpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			adminHandler.handleReindex(ac);
		});
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
	 *            key of the index handlers
	 */
	private <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void registerHandler(String typeName,
			Func0<RootVertex<T>> root, Class<RL> classOfRL, SearchHandler<T, TR> searchHandler, RL exampleListResponse) {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/" + typeName);
		endpoint.method(POST);
		endpoint.description("Invoke a search query for " + typeName + " and return a paged list response.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, exampleListResponse, "Paged search result for " + typeName);
		endpoint.exampleRequest(miscExamples.getSearchQueryExample());
		endpoint.handler(rc -> {
			try {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				searchHandler.query(ac, root, classOfRL);
			} catch (Exception e) {
				rc.fail(e);
			}
		});
	}

}
