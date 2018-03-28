package com.gentics.mesh.core.endpoint.role;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.EndpointRoute;
import com.gentics.mesh.router.route.AbstractEndpoint;
import com.gentics.mesh.util.UUIDUtil;

public class RoleEndpoint extends AbstractEndpoint {

	private RoleCrudHandler crudHandler;

	public RoleEndpoint() {
		super("roles");
	}

	@Inject
	public RoleEndpoint(RoleCrudHandler crudHandler) {
		super("roles");
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
		EndpointRoute permissionSetEndpoint = createEndpoint();
		permissionSetEndpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)");
		permissionSetEndpoint.setRAMLPath("/{roleUuid}/permissions/{path}");
		permissionSetEndpoint.addUriParameter("roleUuid", "Uuid of the role.", UUIDUtil.randomUUID());
		permissionSetEndpoint.addUriParameter("path", "API path to the element.",
				"projects/" + UUIDUtil.randomUUID() + "\nprojects/" + UUIDUtil.randomUUID() + "/nodes/" + UUIDUtil.randomUUID());
		permissionSetEndpoint.method(POST);
		permissionSetEndpoint.description("Set the permissions between role and the targeted element.");
		permissionSetEndpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Permissions were set.");
		permissionSetEndpoint.exampleRequest(roleExamples.getRolePermissionRequest());
		permissionSetEndpoint.consumes(APPLICATION_JSON);
		permissionSetEndpoint.produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionUpdate(ac, roleUuid, pathToElement);
		});

		EndpointRoute permissionGetEndpoint = createEndpoint();
		permissionGetEndpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)");
		permissionGetEndpoint.setRAMLPath("/{roleUuid}/permissions/{path}");
		permissionGetEndpoint.addUriParameter("roleUuid", "Uuid of the role.", UUIDUtil.randomUUID());
		permissionGetEndpoint.addUriParameter("path", "API path to the element.",
				"projects/" + UUIDUtil.randomUUID() + "\nprojects/" + UUIDUtil.randomUUID() + "/nodes/" + UUIDUtil.randomUUID());
		permissionGetEndpoint.description("Load the permissions between given role and the targeted element.");
		permissionGetEndpoint.method(GET);
		permissionGetEndpoint.produces(APPLICATION_JSON);
		permissionGetEndpoint.exampleResponse(OK, roleExamples.getRolePermissionResponse(), "Loaded permissions.");
		permissionGetEndpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionRead(ac, roleUuid, pathToElement);
		});
	}

	private void addDeleteHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/:roleUuid");
		endpoint.addUriParameter("roleUuid", "Uuid of the role", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.description("Delete the role with the given uuid");
		endpoint.exampleResponse(NO_CONTENT, "Role was deleted.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("roleUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/:roleUuid");
		endpoint.addUriParameter("roleUuid", "Uuid of the role.", UUIDUtil.randomUUID());
		endpoint.description("Update the role with the given uuid. The role is created if no role with the specified uuid could be found.");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.exampleRequest(roleExamples.getRoleUpdateRequest("New role name"));
		endpoint.exampleResponse(OK, roleExamples.getRoleResponse1("New role name"), "Updated role.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("roleUuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addReadHandler() {
		EndpointRoute readOne = createEndpoint();
		readOne.path("/:roleUuid");
		readOne.addUriParameter("roleUuid", "Uuid of the role", UUIDUtil.randomUUID());
		readOne.description("Load the role with the given uuid.");
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, roleExamples.getRoleResponse1("Admin Role"), "Loaded role.");
		readOne.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("roleUuid");
			crudHandler.handleRead(ac, uuid);
		});

		/*
		 * List all roles when no parameter was specified
		 */
		EndpointRoute readAll = createEndpoint();
		readAll.path("/");
		readAll.description("Load multiple roles and return a paged list response");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.exampleResponse(OK, roleExamples.getRoleListResponse(), "Loaded list of roles.");
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			crudHandler.handleReadList(ac);
		});
	}

	private void addCreateHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.description("Create a new role.");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(roleExamples.getRoleCreateRequest("New role"));
		endpoint.exampleResponse(CREATED, roleExamples.getRoleResponse1("New role"), "Created role.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			crudHandler.handleCreate(ac);
		});
	}
}
