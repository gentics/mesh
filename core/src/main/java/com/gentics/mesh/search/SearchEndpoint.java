package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
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
import com.gentics.mesh.search.index.IndexHandler;

import dagger.Lazy;
import rx.functions.Func0;

@Singleton
public class SearchEndpoint extends AbstractEndpoint {

	private SearchRestHandler searchHandler;

	private IndexHandlerRegistry registry;

	private Lazy<BootstrapInitializer> boot;

	@Inject
	public SearchEndpoint(RouterStorage routerStorage, SearchRestHandler searchHandler, IndexHandlerRegistry registry,
			Lazy<BootstrapInitializer> boot) {
		super("search", routerStorage);
		this.searchHandler = searchHandler;
		this.registry = registry;
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
	public void registerEndPoints() throws Exception {
		secureAll();
		addSearchEndpoints();
	}

	/**
	 * Add various search endpoints using the aggregation nodes.
	 */
	private void addSearchEndpoints() {
		registerHandler("users", () -> boot.get().meshRoot().getUserRoot(), UserListResponse.class, User.TYPE, userExamples.getUserListResponse());
		registerHandler("groups", () -> boot.get().meshRoot().getGroupRoot(), GroupListResponse.class, Group.TYPE,
				groupExamples.getGroupListResponse());
		registerHandler("roles", () -> boot.get().meshRoot().getRoleRoot(), RoleListResponse.class, Role.TYPE, roleExamples.getRoleListResponse());
		registerHandler("nodes", () -> boot.get().meshRoot().getNodeRoot(), NodeListResponse.class, Node.TYPE, nodeExamples.getNodeListResponse());
		registerHandler("tags", () -> boot.get().meshRoot().getTagRoot(), TagListResponse.class, Tag.TYPE, tagExamples.getTagListResponse());
		registerHandler("tagFamilies", () -> boot.get().meshRoot().getTagFamilyRoot(), TagFamilyListResponse.class, TagFamily.TYPE,
				tagFamilyExamples.getTagFamilyListResponse());
		registerHandler("projects", () -> boot.get().meshRoot().getProjectRoot(), ProjectListResponse.class, Project.TYPE,
				projectExamples.getProjectListResponse());
		registerHandler("schemas", () -> boot.get().meshRoot().getSchemaContainerRoot(), SchemaListResponse.class, SchemaContainer.TYPE,
				schemaExamples.getSchemaListResponse());
		registerHandler("microschemas", () -> boot.get().meshRoot().getMicroschemaContainerRoot(), MicroschemaListResponse.class,
				MicroschemaContainer.TYPE, microschemaExamples.getMicroschemaListResponse());
		addAdminHandlers();
	}

	private void addAdminHandlers() {
		Endpoint statusEndpoint = createEndpoint();
		statusEndpoint.path("/status");
		statusEndpoint.method(GET);
		statusEndpoint.description("Returns the search index status.");
		statusEndpoint.produces(APPLICATION_JSON);
		statusEndpoint.exampleResponse(OK, miscExamples.getMessageResponse(), "Search index status.");
		statusEndpoint.handler(rc -> {
			searchHandler.handleStatus(InternalActionContext.create(rc));
		});

		Endpoint clearBatches = createEndpoint();
		clearBatches.path("/clearBatches");
		clearBatches.method(GET);
		clearBatches.produces(APPLICATION_JSON);
		clearBatches.description("Removes the existing search queue batches from the queue.");
		clearBatches.exampleResponse(OK, miscExamples.getMessageResponse(), "Invoked clearing of all batches.");
		clearBatches.handler(rc -> {
			searchHandler.handleClearBatches(InternalActionContext.create(rc));
		});

		Endpoint processBatches = createEndpoint();
		processBatches.path("/processBatches");
		processBatches.method(GET);
		processBatches.produces(APPLICATION_JSON);
		processBatches.description("Invoke batch processing of remaining batches in the queue.");
		processBatches.exampleResponse(OK, miscExamples.getMessageResponse(), "Invoked all remaining batches.");
		processBatches.handler(rc -> {
			searchHandler.handleProcessBatches(InternalActionContext.create(rc));
		});
		
		
		Endpoint createMappings = createEndpoint();
		createMappings.path("/createMappings");
		createMappings.method(GET);
		createMappings.produces(APPLICATION_JSON);
		createMappings.description("Create search index mappings.");
		createMappings.exampleResponse(OK, miscExamples.getMessageResponse(), "Create all mappings.");
		createMappings.handler(rc -> {
			searchHandler.createMappings(InternalActionContext.create(rc));
		});

		Endpoint reindexEndpoint = createEndpoint();
		reindexEndpoint.path("/reindex");
		reindexEndpoint.method(GET);
		reindexEndpoint.produces(APPLICATION_JSON);
		reindexEndpoint.description("Invokes a full reindex of the search indices. This operation may take some time to complete.");
		reindexEndpoint.exampleResponse(OK, miscExamples.getMessageResponse(), "Invoked reindex command for all elements.");
		reindexEndpoint.handler(rc -> {
			searchHandler.handleReindex(InternalActionContext.create(rc));
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
			Func0<RootVertex<T>> root, Class<RL> classOfRL, String indexHandlerKey, RL exampleListResponse) {
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
				IndexHandler indexHandler = registry.get(indexHandlerKey);
				InternalActionContext ac = InternalActionContext.create(rc);
				searchHandler.handleSearch(ac, root, classOfRL, indexHandler.getSelectedIndices(ac), indexHandler.getReadPermission(ac));
			} catch (Exception e) {
				// fail(rc, "search_error_query");
				rc.fail(e);
			}
		});
	}

}
