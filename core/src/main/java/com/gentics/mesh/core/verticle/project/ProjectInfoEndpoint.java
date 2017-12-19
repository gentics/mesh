package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.EndpointRoute;

import io.vertx.core.http.HttpMethod;

public class ProjectInfoEndpoint extends AbstractEndpoint {

	private ProjectCrudHandler crudHandler;

	@Inject
	public ProjectInfoEndpoint(ProjectCrudHandler crudHandler) {
		super(null);
		this.crudHandler = crudHandler;
	}

	public ProjectInfoEndpoint() {
		super("");
	}

	@Override
	public void init(RouterStorage rs) {
		localRouter = rs.getAPIRouter();
	}

	@Override
	public void registerEndPoints() {
		secureAll();
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/:project");
		endpoint.method(HttpMethod.GET);
		endpoint.addUriParameter("project", "Name of the project.", "demo");
		endpoint.description("Return the current project info.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, projectExamples.getProjectResponse("demo"), "Project information.");
		endpoint.handler(rc -> {
			String projectName = rc.request().params().get("project");
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			crudHandler.handleReadByName(ac, projectName);
		});
	}

	@Override
	public String getDescription() {
		return "Project specific endpoints.";
	}

}
