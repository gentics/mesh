package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;


/**
 * Endpoint for /api/v1/:projectName
 */
public class ProjectInfoEndpoint extends AbstractInternalEndpoint {

	private ProjectCrudHandler crudHandler;

	@Inject
	public ProjectInfoEndpoint(MeshAuthChain chain, ProjectCrudHandler crudHandler, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super(null, chain, localConfigApi, db, options);
		this.crudHandler = crudHandler;
	}

	public ProjectInfoEndpoint() {
		super("", null, null, null, null);
	}

	@Override
	public void init(Vertx vertx, RouterStorage rs) {
		localRouter = rs.root().apiRouter().getRouter();
	}

	@Override
	public void registerEndPoints() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:project");
		endpoint.method(HttpMethod.GET);
		endpoint.addUriParameter("project", "Name of the project.", "demo");
		endpoint.description("Return the current project info.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, projectExamples.getProjectResponse("demo"), "Project information.");
		if (chain != null) {
			chain.secure(endpoint.getRoute());
		}
		endpoint.blockingHandler(rc -> {
			String projectName = rc.request().params().get("project");
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadByName(ac, projectName);
		}, false);
	}

	@Override
	public String getDescription() {
		return "Project specific endpoints.";
	}

}
