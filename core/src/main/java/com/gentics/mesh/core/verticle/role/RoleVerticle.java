package com.gentics.mesh.core.verticle.role;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.Endpoint;

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
		permissionSetEndpoint.setRAMLPath("/:uuid/permissions/:path");
		permissionSetEndpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)");
		permissionSetEndpoint.method(PUT);
		permissionSetEndpoint.description("Set the permissions between role and the targeted element.");
		permissionSetEndpoint.exampleResponse(200, miscExamples.getMessageResponse());
		permissionSetEndpoint.exampleRequest(roleExamples.getRolePermissionRequest());
		permissionSetEndpoint.consumes(APPLICATION_JSON);
		permissionSetEndpoint.produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionUpdate(ac, roleUuid, pathToElement);
		});

		Endpoint permissionGetEndpoint = createEndpoint();
		permissionGetEndpoint.setRAMLPath("/:uuid/permissions/:path");
		permissionGetEndpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)");
		permissionGetEndpoint.description("Load the permissions between given role and the targeted element.");
		permissionGetEndpoint.method(GET);
		permissionGetEndpoint.produces(APPLICATION_JSON);
		permissionGetEndpoint.exampleResponse(200, roleExamples.getRolePermissionResponse());
		permissionGetEndpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionRead(ac, roleUuid, pathToElement);
		});
	}

	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid");
		endpoint.method(DELETE);
		endpoint.description("Delete the role with the given uuid");
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid");
		endpoint.method(PUT);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.description("Update the role with the given uuid.");
		endpoint.exampleRequest(roleExamples.getRoleUpdateRequest("New role name"));
		endpoint.exampleResponse(200, roleExamples.getRoleResponse1("New role name"));
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:uuid");
		readOne.method(GET);
		readOne.description("Load the role with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(200, roleExamples.getRoleResponse1("Admin Role"));
		readOne.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
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
		readAll.exampleResponse(200, roleExamples.getRoleListResponse());
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
		endpoint.exampleResponse(201, roleExamples.getRoleResponse1("New role"));
		endpoint.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}
}
