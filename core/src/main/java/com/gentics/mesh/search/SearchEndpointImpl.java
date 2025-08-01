package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.util.Arrays;
import java.util.function.Function;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
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
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.IndexMaintenanceParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.parameter.impl.BranchParametersImpl;
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

/**
 * @see SearchEndpoint
 */
public class SearchEndpointImpl extends AbstractInternalEndpoint implements SearchEndpoint {

	protected final AdminIndexHandler adminHandler;

	protected final UserSearchHandler userSearchHandler;

	protected final GroupSearchHandler groupSearchHandler;

	protected final RoleSearchHandler roleSearchHandler;

	protected final NodeSearchHandler nodeSearchHandler;

	protected final TagSearchHandler tagSearchHandler;

	protected final TagFamilySearchHandler tagFamilySearchHandler;

	protected final ProjectSearchHandler projectSearchHandler;

	protected final SchemaSearchHandler schemaContainerSearchHandler;

	protected final MicroschemaSearchHandler microschemaContainerSearchHandler;

	@Inject
	public SearchEndpointImpl(MeshAuthChain chain, NodeSearchHandler searchHandler, AdminIndexHandler adminHandler,
			UserSearchHandler userSearchHandler, GroupSearchHandler groupSearchHandler,
			RoleSearchHandler roleSearchHandler, NodeSearchHandler nodeSearchHandler, TagSearchHandler tagSearchHandler,
			TagFamilySearchHandler tagFamilySearchHandler, ProjectSearchHandler projectSearchHandler,
			SchemaSearchHandler schemaContainerSearchHandler,
			MicroschemaSearchHandler microschemaContainerSearchHandler, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super("search", chain, localConfigApi, db, options);
		this.adminHandler = adminHandler;
		this.userSearchHandler = userSearchHandler;
		this.groupSearchHandler = groupSearchHandler;
		this.roleSearchHandler = roleSearchHandler;
		this.nodeSearchHandler = nodeSearchHandler;
		this.tagSearchHandler = tagSearchHandler;
		this.tagFamilySearchHandler = tagFamilySearchHandler;
		this.projectSearchHandler = projectSearchHandler;
		this.schemaContainerSearchHandler = schemaContainerSearchHandler;
		this.microschemaContainerSearchHandler = microschemaContainerSearchHandler;
	}

	public SearchEndpointImpl() {
		super("search", null, null, null, null);
		this.adminHandler = null;
		this.userSearchHandler = null;
		this.groupSearchHandler = null;
		this.roleSearchHandler = null;
		this.nodeSearchHandler = null;
		this.tagSearchHandler = null;
		this.tagFamilySearchHandler = null;
		this.projectSearchHandler = null;
		this.schemaContainerSearchHandler = null;
		this.microschemaContainerSearchHandler = null;
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
		registerHandler("users", (uuid) -> Tx.get().userDao().findByUuid(uuid), UserListResponse.class, userSearchHandler,
			userExamples.getUserListResponse(), false);
		registerHandler("groups", (uuid) -> Tx.get().groupDao().findByUuid(uuid), GroupListResponse.class, groupSearchHandler,
			groupExamples.getGroupListResponse(), false);
		registerHandler("roles", (uuid) -> Tx.get().roleDao().findByUuid(uuid), RoleListResponse.class, roleSearchHandler,
			roleExamples.getRoleListResponse(), false);

		registerHandler("nodes", (uuid) -> {
			HibNode node = Tx.get().nodeDao().findByUuidGlobal(uuid);
			return node;
		}, NodeListResponse.class, nodeSearchHandler, nodeExamples.getNodeListResponse(), true, GenericParametersImpl.class, BranchParametersImpl.class);

		registerHandler("tags", (uuid) -> Tx.get().tagDao().findByUuid(uuid), TagListResponse.class, tagSearchHandler, tagExamples
			.createTagListResponse(), false);
		registerHandler("tagFamilies", (uuid) -> Tx.get().tagFamilyDao().findByUuid(uuid), TagFamilyListResponse.class,
			tagFamilySearchHandler,
			tagFamilyExamples.getTagFamilyListResponse(), false);

		registerHandler("projects", (uuid) -> Tx.get().projectDao().findByUuid(uuid), ProjectListResponse.class,
			projectSearchHandler, projectExamples
				.getProjectListResponse(),
			false);
		registerHandler("schemas", (uuid) -> Tx.get().schemaDao().findByUuid(uuid), SchemaListResponse.class,
			schemaContainerSearchHandler,
			schemaExamples.getSchemaListResponse(), false);
		registerHandler("microschemas", (uuid) -> Tx.get().microschemaDao().findByUuid(uuid), MicroschemaListResponse.class,
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
		}, false);

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
	@SafeVarargs
	private <T extends HibCoreElement<?>, TR extends RestModel, RL extends ListResponse<TR>> void registerHandler(String typeName,
		Function<String, T> elementLoader, Class<RL> classOfRL, SearchHandler<T, TR> searchHandler, RL exampleListResponse,
		boolean filterByLanguage, Class<? extends ParameterProvider>... extraParameters) {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/" + typeName);
		endpoint.method(POST);
		endpoint.setMutating(false);
		endpoint.description("Invoke a search query for " + typeName + " and return a paged list response.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.addQueryParameters(PagingParametersImpl.class);
		endpoint.addQueryParameters(SearchParametersImpl.class);
		Arrays.stream(extraParameters).forEach(endpoint::addQueryParameters);
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
