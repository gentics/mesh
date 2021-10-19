package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.util.function.Function;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
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
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.IndexMaintenanceParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
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

/**
 * @see SearchEndpoint
 */
public class SearchEndpointImpl extends AbstractInternalEndpoint implements SearchEndpoint {

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
	Database db;

	@Inject
	public SearchEndpointImpl(MeshAuthChainImpl chain, NodeSearchHandler searchHandler, Lazy<BootstrapInitializer> boot) {
		super("search", chain);
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

		addSearchEndpoints();
	}

	/**
	 * Add various search endpoints using the aggregation nodes.
	 */
	private void addSearchEndpoints() {
		registerHandler("users", (uuid) -> boot.get().meshRoot().getUserRoot().findByUuid(uuid), UserListResponse.class, userSearchHandler,
			userExamples.getUserListResponse(), false);
		registerHandler("groups", (uuid) -> boot.get().meshRoot().getGroupRoot().findByUuid(uuid), GroupListResponse.class, groupSearchHandler,
			groupExamples.getGroupListResponse(), false);
		registerHandler("roles", (uuid) -> boot.get().meshRoot().getRoleRoot().findByUuid(uuid), RoleListResponse.class, roleSearchHandler,
			roleExamples.getRoleListResponse(), false);

		registerHandler("nodes", (uuid) -> {
			Node node = db.index().findByUuid(NodeImpl.class, uuid);
			return node;
		}, NodeListResponse.class, nodeSearchHandler, nodeExamples.getNodeListResponse(), true);

		registerHandler("tags", (uuid) -> boot.get().meshRoot().getTagRoot().findByUuid(uuid), TagListResponse.class, tagSearchHandler, tagExamples
			.createTagListResponse(), false);
		registerHandler("tagFamilies", (uuid) -> boot.get().meshRoot().getTagFamilyRoot().findByUuid(uuid), TagFamilyListResponse.class,
			tagFamilySearchHandler,
			tagFamilyExamples.getTagFamilyListResponse(), false);

		registerHandler("projects", (uuid) -> boot.get().meshRoot().getProjectRoot().findByUuid(uuid), ProjectListResponse.class,
			projectSearchHandler, projectExamples
				.getProjectListResponse(),
			false);
		registerHandler("schemas", (uuid) -> boot.get().meshRoot().getSchemaContainerRoot().findByUuid(uuid), SchemaListResponse.class,
			schemaContainerSearchHandler,
			schemaExamples.getSchemaListResponse(), false);
		registerHandler("microschemas", (uuid) -> boot.get().meshRoot().getMicroschemaContainerRoot().findByUuid(uuid), MicroschemaListResponse.class,
			microschemaContainerSearchHandler, microschemaExamples.getMicroschemaListResponse(), false);
		addAdminHandlers();
	}

	private void addAdminHandlers() {
		InternalEndpointRoute statusEndpoint = createRoute();
		statusEndpoint.path("/status");
		statusEndpoint.method(GET);
		statusEndpoint.description("Returns the search index status.");
		statusEndpoint.produces(APPLICATION_JSON);
		statusEndpoint.exampleResponse(OK, miscExamples.searchStatusJson(), "Search index status.");
		statusEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			adminHandler.handleStatus(ac);
		});

		// Endpoint createMappings = createEndpoint();
		// createMappings.path("/createMappings");
		// createMappings.method(POST);
		// createMappings.produces(APPLICATION_JSON);
		// createMappings.description("Create search index mappings.");
		// createMappings.exampleResponse(OK, miscExamples.createMessageResponse(), "Create all mappings.");
		// createMappings.handler(rc -> {
		// InternalActionContext ac = wrap(rc);
		// adminHandler.createMappings(ac);
		// });

		InternalEndpointRoute indexClearEndpoint = createRoute();
		indexClearEndpoint.path("/clear");
		indexClearEndpoint.method(POST);
		indexClearEndpoint.produces(APPLICATION_JSON);
		indexClearEndpoint.addQueryParameters(IndexMaintenanceParametersImpl.class);
		indexClearEndpoint.description("Drops all indices and recreates them. The index sync is not invoked automatically.");
		indexClearEndpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Recreated all indices.");
		indexClearEndpoint.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			adminHandler.handleClear(ac);
		});

		InternalEndpointRoute indexSyncEndpoint = createRoute();
		indexSyncEndpoint.path("/sync");
		indexSyncEndpoint.method(POST);
		indexSyncEndpoint.produces(APPLICATION_JSON);
		indexSyncEndpoint.addQueryParameters(IndexMaintenanceParametersImpl.class);
		indexSyncEndpoint.description(
			"Invokes the manual synchronisation of the search indices. This operation may take some time to complete and is performed asynchronously. When clustering is enabled it will be executed on any free instance.");
		indexSyncEndpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Invoked index synchronisation on all indices.");
		indexSyncEndpoint.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			adminHandler.handleSync(ac);
		});
	}

	/**
	 * Register the selected search handler.
	 * 
	 * @param typeName
	 *            Name of the search endpoint
	 * @param elementLoader
	 *            Loader that should used to load the objects that were found within the search index
	 * @param classOfRL
	 *            Class of matching list response
	 */
	private <T extends HibCoreElement, TR extends RestModel, RL extends ListResponse<TR>> void registerHandler(String typeName,
		Function<String, T> elementLoader, Class<RL> classOfRL, SearchHandler<T, TR> searchHandler, RL exampleListResponse,
		boolean filterByLanguage) {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/" + typeName);
		endpoint.method(POST);
		endpoint.setMutating(false);
		endpoint.description("Invoke a search query for " + typeName + " and return a paged list response.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.addQueryParameters(PagingParametersImpl.class);
		endpoint.addQueryParameters(SearchParametersImpl.class);
		endpoint.exampleResponse(OK, exampleListResponse, "Paged search result for " + typeName);
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
