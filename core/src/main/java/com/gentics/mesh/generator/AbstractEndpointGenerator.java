package com.gentics.mesh.generator;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;

import org.mockito.Mockito;

import com.gentics.mesh.core.endpoint.admin.AdminEndpoint;
import com.gentics.mesh.core.endpoint.admin.AdminEndpointImpl;
import com.gentics.mesh.core.endpoint.admin.HealthEndpoint;
import com.gentics.mesh.core.endpoint.admin.RestInfoEndpoint;
import com.gentics.mesh.core.endpoint.auth.AuthenticationEndpoint;
import com.gentics.mesh.core.endpoint.branch.BranchEndpoint;
import com.gentics.mesh.core.endpoint.eventbus.EventbusEndpoint;
import com.gentics.mesh.core.endpoint.group.GroupEndpoint;
import com.gentics.mesh.core.endpoint.microschema.MicroschemaEndpoint;
import com.gentics.mesh.core.endpoint.microschema.ProjectMicroschemaEndpoint;
import com.gentics.mesh.core.endpoint.navroot.NavRootEndpoint;
import com.gentics.mesh.core.endpoint.node.NodeEndpoint;
import com.gentics.mesh.core.endpoint.project.ProjectEndpoint;
import com.gentics.mesh.core.endpoint.project.ProjectInfoEndpoint;
import com.gentics.mesh.core.endpoint.role.RoleEndpoint;
import com.gentics.mesh.core.endpoint.schema.ProjectSchemaEndpoint;
import com.gentics.mesh.core.endpoint.schema.SchemaEndpoint;
import com.gentics.mesh.core.endpoint.tagfamily.TagFamilyEndpoint;
import com.gentics.mesh.core.endpoint.user.UserEndpoint;
import com.gentics.mesh.core.endpoint.utility.UtilityEndpoint;
import com.gentics.mesh.core.endpoint.webroot.WebRootEndpoint;
import com.gentics.mesh.core.endpoint.webrootfield.WebRootFieldEndpoint;
import com.gentics.mesh.graphql.GraphQLEndpoint;
import com.gentics.mesh.router.APIRouterImpl;
import com.gentics.mesh.router.RootRouterImpl;
import com.gentics.mesh.router.RouterStorageImpl;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.gentics.mesh.search.ProjectRawSearchEndpointImpl;
import com.gentics.mesh.search.ProjectSearchEndpointImpl;
import com.gentics.mesh.search.RawSearchEndpointImpl;
import com.gentics.mesh.search.SearchEndpointImpl;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public abstract class AbstractEndpointGenerator<T> extends AbstractGenerator {

	public AbstractEndpointGenerator() {
	}

	public AbstractEndpointGenerator(File outputFolder, boolean cleanup) throws IOException {
		super(outputFolder, cleanup);
	}

	public AbstractEndpointGenerator(File outputFolder) throws IOException {
		super(outputFolder);
	}

	/**
	 * Add all project verticles to the list resources.
	 * 
	 * @param consumer
	 * @throws IOException
	 * @throws Exception
	 */
	protected void addProjectEndpoints(T consumer) throws IOException {
		NodeEndpoint nodeEndpoint = Mockito.spy(new NodeEndpoint());
		initEndpoint(nodeEndpoint);
		String projectBasePath = "/{project}";
		addEndpoints(projectBasePath, consumer, nodeEndpoint, true);

		TagFamilyEndpoint tagFamilyEndpoint = Mockito.spy(new TagFamilyEndpoint());
		initEndpoint(tagFamilyEndpoint);
		addEndpoints(projectBasePath, consumer, tagFamilyEndpoint, true);

		NavRootEndpoint navEndpoint = Mockito.spy(new NavRootEndpoint());
		initEndpoint(navEndpoint);
		addEndpoints(projectBasePath, consumer, navEndpoint, true);

		WebRootEndpoint webEndpoint = Mockito.spy(new WebRootEndpoint());
		initEndpoint(webEndpoint);
		addEndpoints(projectBasePath, consumer, webEndpoint, true);

		WebRootFieldEndpoint webFieldEndpoint = Mockito.spy(new WebRootFieldEndpoint());
		initEndpoint(webFieldEndpoint);
		addEndpoints(projectBasePath, consumer, webFieldEndpoint, true);

		BranchEndpoint branchEndpoint = Mockito.spy(new BranchEndpoint());
		initEndpoint(branchEndpoint);
		addEndpoints(projectBasePath, consumer, branchEndpoint, true);

		GraphQLEndpoint graphqlEndpoint = Mockito.spy(new GraphQLEndpoint());
		initEndpoint(graphqlEndpoint);
		addEndpoints(projectBasePath, consumer, graphqlEndpoint, true);

		ProjectSearchEndpointImpl projectSearchEndpoint = Mockito.spy(new ProjectSearchEndpointImpl());
		initEndpoint(projectSearchEndpoint);
		addEndpoints(projectBasePath, consumer, projectSearchEndpoint, true);

		ProjectRawSearchEndpointImpl projectRawSearchEndpoint = Mockito.spy(new ProjectRawSearchEndpointImpl());
		initEndpoint(projectRawSearchEndpoint);
		addEndpoints(projectBasePath, consumer, projectRawSearchEndpoint, true);

		ProjectSchemaEndpoint projectSchemaEndpoint = Mockito.spy(new ProjectSchemaEndpoint());
		initEndpoint(projectSchemaEndpoint);
		addEndpoints(projectBasePath, consumer, projectSchemaEndpoint, true);

		ProjectMicroschemaEndpoint projectMicroschemaEndpoint = Mockito.spy(new ProjectMicroschemaEndpoint());
		initEndpoint(projectMicroschemaEndpoint);
		addEndpoints(projectBasePath, consumer, projectMicroschemaEndpoint, true);
	}

	/**
	 * Add all core verticles to the map of RAML resources.
	 * 
	 * @param consumer
	 * @throws IOException
	 * @throws Exception
	 */
	protected void addCoreEndpoints(T consumer) throws IOException {
		String coreBasePath = "";
		UserEndpoint userEndpoint = Mockito.spy(new UserEndpoint());
		initEndpoint(userEndpoint);
		addEndpoints(coreBasePath, consumer, userEndpoint, false);

		RoleEndpoint roleEndpoint = Mockito.spy(new RoleEndpoint());
		initEndpoint(roleEndpoint);
		addEndpoints(coreBasePath, consumer, roleEndpoint, false);

		GroupEndpoint groupEndpoint = Mockito.spy(new GroupEndpoint());
		initEndpoint(groupEndpoint);
		addEndpoints(coreBasePath, consumer, groupEndpoint, false);

		ProjectEndpoint projectEndpoint = Mockito.spy(new ProjectEndpoint());
		initEndpoint(projectEndpoint);
		addEndpoints(coreBasePath, consumer, projectEndpoint, false);

		SchemaEndpoint schemaEndpoint = Mockito.spy(new SchemaEndpoint());
		initEndpoint(schemaEndpoint);
		addEndpoints(coreBasePath, consumer, schemaEndpoint, false);

		MicroschemaEndpoint microschemaEndpoint = Mockito.spy(new MicroschemaEndpoint());
		initEndpoint(microschemaEndpoint);
		addEndpoints(coreBasePath, consumer, microschemaEndpoint, false);

		AdminEndpoint adminEndpoint = Mockito.spy(new AdminEndpointImpl());
		initEndpoint(adminEndpoint);
		addEndpoints(coreBasePath, consumer, adminEndpoint, false);

		HealthEndpoint healthEndpoint = Mockito.spy(new HealthEndpoint());
		initEndpoint(healthEndpoint);
		addEndpoints(coreBasePath, consumer, healthEndpoint, false);

		SearchEndpointImpl searchEndpoint = Mockito.spy(new SearchEndpointImpl());
		initEndpoint(searchEndpoint);
		addEndpoints(coreBasePath, consumer, searchEndpoint, false);

		RawSearchEndpointImpl rawSearchEndpoint = Mockito.spy(new RawSearchEndpointImpl());
		initEndpoint(rawSearchEndpoint);
		addEndpoints(coreBasePath, consumer, rawSearchEndpoint, false);

		UtilityEndpoint utilityEndpoint = Mockito.spy(new UtilityEndpoint());
		initEndpoint(utilityEndpoint);
		addEndpoints(coreBasePath, consumer, utilityEndpoint, false);

		AuthenticationEndpoint authEndpoint = Mockito.spy(new AuthenticationEndpoint());
		initEndpoint(authEndpoint);
		addEndpoints(coreBasePath, consumer, authEndpoint, false);

		EventbusEndpoint eventbusEndpoint = Mockito.spy(new EventbusEndpoint());
		initEndpoint(eventbusEndpoint);
		addEndpoints(coreBasePath, consumer, eventbusEndpoint, false);

		RouterStorageImpl rs = Mockito.mock(RouterStorageImpl.class);
		RootRouterImpl rootRouter = Mockito.mock(RootRouterImpl.class);
		Mockito.when(rs.root()).thenReturn(rootRouter);
		APIRouterImpl apiRouter = Mockito.mock(APIRouterImpl.class);
		Mockito.when(rootRouter.apiRouter()).thenReturn(apiRouter);
		RestInfoEndpoint infoEndpoint = Mockito.spy(new RestInfoEndpoint(""));
		infoEndpoint.init(null, rs);
		initEndpoint(infoEndpoint);
		addEndpoints(coreBasePath, consumer, infoEndpoint, false);

		ProjectInfoEndpoint projectInfoEndpoint = Mockito.spy(new ProjectInfoEndpoint());
		initEndpoint(projectInfoEndpoint);
		addEndpoints(coreBasePath, consumer, projectInfoEndpoint, false);
	}

	protected abstract void addEndpoints(String coreBasePath, T consumer, AbstractInternalEndpoint projectInfoEndpoint, boolean isProject) throws IOException;

	protected void initEndpoint(AbstractInternalEndpoint endpoint) {
		Vertx vertx = mock(Vertx.class);
		Mockito.when(endpoint.getRouter()).thenReturn(Router.router(vertx));
		endpoint.registerEndPoints();
	}
}
