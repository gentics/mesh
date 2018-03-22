package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.gentics.mesh.util.UUIDUtil;

public class ProjectEndpoint extends AbstractInternalEndpoint {

	private ProjectCrudHandler crudHandler;

	@Inject
	public ProjectEndpoint(ProjectCrudHandler crudHandler) {
		super("projects");
		this.crudHandler = crudHandler;
	}

	public ProjectEndpoint() {
		super("projects");
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of projects.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addUpdateHandler() {
		InternalEndpointRoute updateEndpoint = createRoute();
		updateEndpoint.path("/:projectUuid");
		updateEndpoint
				.description("Update the project with the given uuid. The project is created if no project with the specified uuid could be found.");
		updateEndpoint.addUriParameter("projectUuid", "Uuid of the project.", UUIDUtil.randomUUID());
		updateEndpoint.method(POST);
		updateEndpoint.consumes(APPLICATION_JSON);
		updateEndpoint.produces(APPLICATION_JSON);
		updateEndpoint.exampleRequest(projectExamples.getProjectUpdateRequest("New project name"));
		updateEndpoint.exampleResponse(OK, projectExamples.getProjectResponse("New project name"), "Updated project.");
		updateEndpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("projectUuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	// TODO when the root tag is not saved the project can't be saved. Unfortunately this did not show up as an http error. We must handle those
	// cases. They must show up in any case.
	private void addCreateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new project.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(projectExamples.getProjectCreateRequest("New project"));
		endpoint.exampleResponse(CREATED, projectExamples.getProjectResponse("New Project"), "Created project.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			crudHandler.handleCreate(ac);
		});
	}

	private void addReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:projectUuid");
		readOne.addUriParameter("projectUuid", "Uuid of the project.", UUIDUtil.randomUUID());
		readOne.description("Load the project with the given uuid.");
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, projectExamples.getProjectResponse("Project name"), "Loaded project.");
		readOne.addQueryParameters(RolePermissionParametersImpl.class);
		readOne.handler(rc -> {
			String uuid = rc.request().params().get("projectUuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				crudHandler.handleRead(ac, uuid);
			}
		});

		InternalEndpointRoute readAll = createRoute();
		readAll.path("/");
		readAll.method(GET);
		readAll.description("Load multiple projects and return a paged response.");
		readAll.produces(APPLICATION_JSON);
		readAll.exampleResponse(OK, projectExamples.getProjectListResponse(), "Loaded project list.");
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.addQueryParameters(RolePermissionParametersImpl.class);
		readAll.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			crudHandler.handleReadList(ac);
		});
	}

	private void addDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:projectUuid");
		endpoint.addUriParameter("projectUuid", "Uuid of the project.", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.description("Delete the project and all attached nodes.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(NO_CONTENT, "Project was deleted.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("projectUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}
}
