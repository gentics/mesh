package com.gentics.mesh.core.verticle.role;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

@Component
@Scope("singleton")
@SpringVerticle
public class RoleVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private RoleCrudHandler crudHandler;

	public RoleVerticle() {
		super("roles");
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of roles.";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();

		addPermissionHandler();
	}

	private void addPermissionHandler() {
		Endpoint permissionSetEndpoint = createEndpoint();
		permissionSetEndpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)");
		permissionSetEndpoint.setRAMLPath("/{roleUuid}/permissions/{pathToElement}");
		permissionSetEndpoint.addUriParameter("roleUuid", "Uuid of the role.", UUIDUtil.randomUUID());
		permissionSetEndpoint.addUriParameter("pathToElement", "API path to the element.", "projects/" + UUIDUtil.randomUUID());
		permissionSetEndpoint.method(POST);
		permissionSetEndpoint.description("Set the permissions between role and the targeted element.");
		permissionSetEndpoint.exampleResponse(OK, miscExamples.getMessageResponse(), "Permissions were set.");
		permissionSetEndpoint.exampleRequest(roleExamples.getRolePermissionRequest());
		permissionSetEndpoint.consumes(APPLICATION_JSON);
		permissionSetEndpoint.produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionUpdate(ac, roleUuid, pathToElement);
		});

		Endpoint permissionGetEndpoint = createEndpoint();
		permissionGetEndpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)");
		permissionGetEndpoint.setRAMLPath("/{roleUuid}/permissions/{pathToElement}");
		permissionGetEndpoint.addUriParameter("roleUuid", "Uuid of the role.", UUIDUtil.randomUUID());
		permissionGetEndpoint.addUriParameter("pathToElement", "API path to the element.", "projects/" + UUIDUtil.randomUUID());
		permissionGetEndpoint.description("Load the permissions between given role and the targeted element.");
		permissionGetEndpoint.method(GET);
		permissionGetEndpoint.produces(APPLICATION_JSON);
		permissionGetEndpoint.exampleResponse(OK, roleExamples.getRolePermissionResponse(), "Loaded permissions.");
		permissionGetEndpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionRead(ac, roleUuid, pathToElement);
		});
	}

	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:roleUuid");
		endpoint.addUriParameter("roleUuid", "Uuid of the role", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.description("Delete the role with the given uuid");
		endpoint.exampleResponse(NO_CONTENT, "Role was deleted.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("roleUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:roleUuid");
		endpoint.addUriParameter("roleUuid", "Uuid of the role.", UUIDUtil.randomUUID());
		endpoint.description("Update the role with the given uuid.");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.exampleRequest(roleExamples.getRoleUpdateRequest("New role name"));
		endpoint.exampleResponse(OK, roleExamples.getRoleResponse1("New role name"), "Updated role.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("roleUuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:roleUuid");
		readOne.addUriParameter("roleUuid", "Uuid of the role", UUIDUtil.randomUUID());
		readOne.description("Load the role with the given uuid.");
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, roleExamples.getRoleResponse1("Admin Role"), "Loaded role.");
		readOne.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("roleUuid");
			crudHandler.handleRead(ac, uuid);
		});

		/*
		 * List all roles when no parameter was specified
		 */
		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.description("Load multiple roles and return a paged list response");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.exampleResponse(OK, roleExamples.getRoleListResponse(), "Loaded list of roles.");
		readAll.addQueryParameters(PagingParameters.class);
		readAll.handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addCreateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.description("Create a new role.");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(roleExamples.getRoleCreateRequest("New role"));
		endpoint.exampleResponse(CREATED, roleExamples.getRoleResponse1("New role"), "Created role.");
		endpoint.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}
}
