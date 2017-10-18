package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.IndexHandler;
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
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.schema.SchemaContainerIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;

import dagger.Lazy;

@Singleton
public class SearchEndpoint extends AbstractEndpoint {

	private SearchRestHandler searchHandler;

	private Lazy<BootstrapInitializer> boot;

	@Inject
	NodeIndexHandler nodeIndexHandler;

	@Inject
	UserIndexHandler userIndexHandler;

	@Inject
	GroupIndexHandler groupIndexHandler;

	@Inject
	RoleIndexHandler roleIndexHandler;

	@Inject
	ProjectIndexHandler projectIndexHandler;

	@Inject
	TagFamilyIndexHandler tagFamilyIndexHandler;

	@Inject
	TagIndexHandler tagIndexHandler;

	@Inject
	SchemaContainerIndexHandler schemaContainerIndexHandler;

	@Inject
	MicroschemaContainerIndexHandler microschemaContainerIndexHandler;

	@Inject
	public SearchEndpoint(RouterStorage routerStorage, SearchRestHandler searchHandler, Lazy<BootstrapInitializer> boot) {
		super("search", routerStorage);
		this.searchHandler = searchHandler;
		this.boot = boot;
	}

	public SearchEndpoint() {
		super("search", null);
	}

	@Override
	public String getDescription() {
		return "Provides search endpoints which can be used to invoke global searches";
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
		registerHandler("users", () -> boot.get().meshRoot().getUserRoot(), UserListResponse.class, userIndexHandler,
				userExamples.getUserListResponse());
		registerHandler("groups", () -> boot.get().meshRoot().getGroupRoot(), GroupListResponse.class, groupIndexHandler,
				groupExamples.getGroupListResponse());
		registerHandler("roles", () -> boot.get().meshRoot().getRoleRoot(), RoleListResponse.class, roleIndexHandler,
				roleExamples.getRoleListResponse());
		registerHandler("nodes", () -> boot.get().meshRoot().getNodeRoot(), NodeListResponse.class, nodeIndexHandler,
				nodeExamples.getNodeListResponse());
		registerHandler("tags", () -> boot.get().meshRoot().getTagRoot(), TagListResponse.class, tagIndexHandler, tagExamples.createTagListResponse());
		registerHandler("tagFamilies", () -> boot.get().meshRoot().getTagFamilyRoot(), TagFamilyListResponse.class, tagFamilyIndexHandler,
				tagFamilyExamples.getTagFamilyListResponse());
		registerHandler("projects", () -> boot.get().meshRoot().getProjectRoot(), ProjectListResponse.class, projectIndexHandler,
				projectExamples.getProjectListResponse());
		registerHandler("schemas", () -> boot.get().meshRoot().getSchemaContainerRoot(), SchemaListResponse.class, schemaContainerIndexHandler,
				schemaExamples.getSchemaListResponse());
		registerHandler("microschemas", () -> boot.get().meshRoot().getMicroschemaContainerRoot(), MicroschemaListResponse.class,
				microschemaContainerIndexHandler, microschemaExamples.getMicroschemaListResponse());
		addAdminHandlers();
	}

	private void addAdminHandlers() {
		Endpoint statusEndpoint = createEndpoint();
		statusEndpoint.path("/status");
		statusEndpoint.method(GET);
		statusEndpoint.description("Returns the search index status.");
		statusEndpoint.produces(APPLICATION_JSON);
		statusEndpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Search index status.");
		statusEndpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			searchHandler.handleStatus(ac);
		});

		Endpoint createMappings = createEndpoint();
		createMappings.path("/createMappings");
		createMappings.method(POST);
		createMappings.produces(APPLICATION_JSON);
		createMappings.description("Create search index mappings.");
		createMappings.exampleResponse(OK, miscExamples.createMessageResponse(), "Create all mappings.");
		createMappings.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			searchHandler.createMappings(ac);
		});

		Endpoint reindexEndpoint = createEndpoint();
		reindexEndpoint.path("/reindex");
		reindexEndpoint.method(POST);
		reindexEndpoint.produces(APPLICATION_JSON);
		reindexEndpoint.description("Invokes a full reindex of the search indices. This operation may take some time to complete.");
		reindexEndpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Invoked reindex command for all elements.");
		reindexEndpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			searchHandler.handleReindex(ac);
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
	private <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void registerHandler(String typeName, Supplier
			<RootVertex<T>> root, Class<RL> classOfRL, IndexHandler indexHandler, RL exampleListResponse) {
		Endpoint endpoint = createEndpoint();
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
				searchHandler.handleSearch(ac, root, classOfRL, indexHandler.getSelectedIndices(ac), indexHandler.getReadPermission(ac));
			} catch (Exception e) {
				// fail(rc, "search_error_query");
				rc.fail(e);
			}
		});
	}

}
