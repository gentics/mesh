package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

@Component
@Scope("singleton")
@SpringVerticle
public class ProjectVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private ProjectCrudHandler crudHandler;

	public ProjectVerticle() {
		super("projects");
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of projects.";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addUpdateHandler() {
		Endpoint updateEndpoint = createEndpoint();
		updateEndpoint.path("/:projectUuid");
		updateEndpoint.description("Update the project with the given uuid.");
		updateEndpoint.addUriParameter("projectUuid", "Uuid of the project.", UUIDUtil.randomUUID());
		updateEndpoint.method(POST);
		updateEndpoint.consumes(APPLICATION_JSON);
		updateEndpoint.produces(APPLICATION_JSON);
		updateEndpoint.exampleRequest(projectExamples.getProjectUpdateRequest("New project name"));
		updateEndpoint.exampleResponse(OK, projectExamples.getProjectResponse("New project name"), "Updated project.");
		updateEndpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("projectUuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	// TODO when the root tag is not saved the project can't be saved. Unfortunately this did not show up as an http error. We must handle those
	// cases. They must show up in any case.
	private void addCreateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new project.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(projectExamples.getProjectCreateRequest("New project"));
		endpoint.exampleResponse(CREATED, projectExamples.getProjectResponse("New Project"), "Created project.");
		endpoint.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}

	private void addReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:projectUuid");
		readOne.addUriParameter("projectUuid", "Uuid of the project.", UUIDUtil.randomUUID());
		readOne.description("Load the project with the given uuid.");
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, projectExamples.getProjectResponse("Project name"), "Loaded project.");
		readOne.addQueryParameters(RolePermissionParameters.class);
		readOne.handler(rc -> {
			String uuid = rc.request().params().get("projectUuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				crudHandler.handleRead(InternalActionContext.create(rc), uuid);
			}
		});

		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.method(GET);
		readAll.description("Load multiple projects and return a paged response.");
		readAll.produces(APPLICATION_JSON);
		readAll.exampleResponse(OK, projectExamples.getProjectListResponse(), "Loaded project list.");
		readAll.addQueryParameters(PagingParameters.class);
		readAll.addQueryParameters(RolePermissionParameters.class);
		readAll.handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:projectUuid");
		endpoint.addUriParameter("projectUuid", "Uuid of the project.", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.description("Delete the project and all attached nodes.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(NO_CONTENT, "Project was deleted.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("projectUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}
}
