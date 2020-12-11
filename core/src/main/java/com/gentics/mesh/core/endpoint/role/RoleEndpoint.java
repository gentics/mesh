package com.gentics.mesh.core.endpoint.role;

import static com.gentics.mesh.core.rest.MeshEvent.ROLE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_PERMISSIONS_CHANGED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_UPDATED;
import static com.gentics.mesh.example.ExampleUuids.NODE_DELOREAN_UUID;
import static com.gentics.mesh.example.ExampleUuids.PROJECT_DEMO_UUID;
import static com.gentics.mesh.example.ExampleUuids.ROLE_CLIENT_UUID;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

/**
 * Endpoint for /api/v1/roles
 */
public class RoleEndpoint extends AbstractInternalEndpoint {

	private RoleCrudHandlerImpl crudHandler;

	public RoleEndpoint() {
		super("roles", null);
	}

	@Inject
	public RoleEndpoint(MeshAuthChainImpl chain, RoleCrudHandlerImpl crudHandler) {
		super("roles", chain);
		this.crudHandler = crudHandler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of roles.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();

		addPermissionHandler();
	}

	private void addPermissionHandler() {
		InternalEndpointRoute permissionSetEndpoint = createRoute();
		permissionSetEndpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)");
		permissionSetEndpoint.setRAMLPath("/{roleUuid}/permissions/{path}");
		permissionSetEndpoint.addUriParameter("roleUuid", "Uuid of the role.", ROLE_CLIENT_UUID);
		permissionSetEndpoint.addUriParameter("path", "API path to the element.",
			"projects/" + PROJECT_DEMO_UUID + "\nprojects/" + PROJECT_DEMO_UUID + "/nodes/" + NODE_DELOREAN_UUID);
		permissionSetEndpoint.method(POST);
		permissionSetEndpoint.description("Set the permissions between role and the targeted element.");
		permissionSetEndpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Permissions were set.");
		permissionSetEndpoint.exampleRequest(roleExamples.getRolePermissionRequest());
		permissionSetEndpoint.consumes(APPLICATION_JSON);
		permissionSetEndpoint.events(ROLE_PERMISSIONS_CHANGED);
		permissionSetEndpoint.produces(APPLICATION_JSON);
		permissionSetEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionUpdate(ac, roleUuid, pathToElement);
		});

		InternalEndpointRoute permissionGetEndpoint = createRoute();
		permissionGetEndpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)");
		permissionGetEndpoint.setRAMLPath("/{roleUuid}/permissions/{path}");
		permissionGetEndpoint.addUriParameter("roleUuid", "Uuid of the role.", ROLE_CLIENT_UUID);
		permissionGetEndpoint.addUriParameter("path", "API path to the element.",
			"projects/" + PROJECT_DEMO_UUID + "\nprojects/" + PROJECT_DEMO_UUID + "/nodes/" + NODE_DELOREAN_UUID);
		permissionGetEndpoint.description("Load the permissions between given role and the targeted element.");
		permissionGetEndpoint.method(GET);
		permissionGetEndpoint.produces(APPLICATION_JSON);
		permissionGetEndpoint.exampleResponse(OK, roleExamples.getRolePermissionResponse(), "Loaded permissions.");
		permissionGetEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionRead(ac, roleUuid, pathToElement);
		});
	}

	private void addDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:roleUuid");
		endpoint.addUriParameter("roleUuid", "Uuid of the role", ROLE_CLIENT_UUID);
		endpoint.method(DELETE);
		endpoint.description("Delete the role with the given uuid");
		endpoint.exampleResponse(NO_CONTENT, "Role was deleted.");
		endpoint.events(ROLE_DELETED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("roleUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:roleUuid");
		endpoint.addUriParameter("roleUuid", "Uuid of the role.", ROLE_CLIENT_UUID);
		endpoint.description("Update the role with the given uuid. The role is created if no role with the specified uuid could be found.");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.exampleRequest(roleExamples.getRoleUpdateRequest("New role name"));
		endpoint.exampleResponse(OK, roleExamples.getRoleResponse1("New role name"), "Updated role.");
		endpoint.events(ROLE_UPDATED, ROLE_CREATED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("roleUuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:roleUuid");
		readOne.addUriParameter("roleUuid", "Uuid of the role", ROLE_CLIENT_UUID);
		readOne.description("Load the role with the given uuid.");
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, roleExamples.getRoleResponse1("Admin Role"), "Loaded role.");
		readOne.addQueryParameters(GenericParametersImpl.class);
		readOne.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("roleUuid");
			crudHandler.handleRead(ac, uuid);
		});

		/*
		 * List all roles when no parameter was specified
		 */
		InternalEndpointRoute readAll = createRoute();
		readAll.path("/");
		readAll.description("Load multiple roles and return a paged list response");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.exampleResponse(OK, roleExamples.getRoleListResponse(), "Loaded list of roles.");
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.addQueryParameters(GenericParametersImpl.class);
		readAll.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadList(ac);
		});
	}

	private void addCreateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/");
		endpoint.description("Create a new role.");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(roleExamples.getRoleCreateRequest("New role"));
		endpoint.exampleResponse(CREATED, roleExamples.getRoleResponse1("New role"), "Created role.");
		endpoint.events(ROLE_CREATED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleCreate(ac);
		});
	}
}
