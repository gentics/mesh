package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;
import static com.gentics.mesh.example.ExampleUuids.PROJECT_DEMO_UUID;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

public class ProjectEndpoint extends AbstractInternalEndpoint {

	private ProjectCrudHandler crudHandler;

	@Inject
	public ProjectEndpoint(MeshAuthChain chain, ProjectCrudHandler crudHandler) {
		super("projects", chain);
		this.crudHandler = crudHandler;
	}

	public ProjectEndpoint() {
		super("projects", null);
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

		// Version purge
		addVersionPurgeHandler();
	}

	private void addUpdateHandler() {
		InternalEndpointRoute updateEndpoint = createRoute();
		updateEndpoint.path("/:projectUuid");
		updateEndpoint
			.description("Update the project with the given uuid. The project is created if no project with the specified uuid could be found.");
		updateEndpoint.addUriParameter("projectUuid", "Uuid of the project.", PROJECT_DEMO_UUID);
		updateEndpoint.method(POST);
		updateEndpoint.consumes(APPLICATION_JSON);
		updateEndpoint.produces(APPLICATION_JSON);
		updateEndpoint.exampleRequest(projectExamples.getProjectUpdateRequest("New project name"));
		updateEndpoint.exampleResponse(OK, projectExamples.getProjectResponse("New project name"), "Updated project.");
		updateEndpoint.events(PROJECT_UPDATED);
		updateEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
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
		endpoint.events(PROJECT_CREATED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleCreate(ac);
		});
	}

	private void addReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:projectUuid");
		readOne.addUriParameter("projectUuid", "Uuid of the project.", PROJECT_DEMO_UUID);
		readOne.description("Load the project with the given uuid.");
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, projectExamples.getProjectResponse("Project name"), "Loaded project.");
		readOne.addQueryParameters(RolePermissionParametersImpl.class);
		readOne.blockingHandler(rc -> {
			String uuid = rc.request().params().get("projectUuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				InternalActionContext ac = wrap(rc);
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
		readAll.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadList(ac);
		});
	}

	private void addDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:projectUuid");
		endpoint.addUriParameter("projectUuid", "Uuid of the project.", PROJECT_DEMO_UUID);
		endpoint.method(DELETE);
		endpoint.description("Delete the project and all attached nodes, tagfamiles and branches.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(NO_CONTENT, "Project was deleted.");
		endpoint.events(PROJECT_DELETED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("projectUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addVersionPurgeHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:projectUuid/maintenance/purge");
		endpoint.addUriParameter("projectUuid", "Uuid of the project.", PROJECT_DEMO_UUID);
		endpoint.method(POST);
		endpoint.description("Invoke a version purge of the project.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, "Project was purged.");
		//TODO handle event
		//endpoint.events(PROJECT_PURGED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("projectUuid");
			crudHandler.handlePurge(ac, uuid);
		});
	}
}
