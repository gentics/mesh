package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.rest.Endpoint;

import io.vertx.ext.web.Router;

@Component
@Scope(value = "singleton")
@SpringVerticle
public class ProjectInfoVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private ProjectCrudHandler crudHandler;

	public ProjectInfoVerticle() {
		super(null);
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:projectName");
		endpoint.description("Return the current project info");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(200, projectExamples.getProjectResponse("dummy"));
		endpoint.handler(rc -> {
			String projectName = rc.request().params().get("projectName");
			crudHandler.handleReadByName(InternalActionContext.create(rc), projectName);
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
