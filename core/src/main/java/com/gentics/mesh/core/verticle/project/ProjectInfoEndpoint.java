package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.Endpoint;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;


@Singleton
public class ProjectInfoEndpoint extends AbstractEndpoint {

	private ProjectCrudHandler crudHandler;

	@Inject
	public ProjectInfoEndpoint(RouterStorage routerStorage, ProjectCrudHandler crudHandler) {
		super(null, routerStorage);
		this.crudHandler = crudHandler;
	}

	public ProjectInfoEndpoint() {
		super("", null);
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		Endpoint endpoint = createEndpoint();
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

	@Override
	public Router setupLocalRouter() {
		return routerStorage.getAPIRouter();
	}

}
