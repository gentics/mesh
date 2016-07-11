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
	public void registerEndPoints() throws Exception {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/*");
		endpoint.handler(getSpringConfiguration().authHandler());

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();

		addPermissionHandler();
	}

	private void addPermissionHandler() {
		Endpoint permissionSetEndpoint = createEndpoint();
		permissionSetEndpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON)
				.handler(rc -> {
					InternalActionContext ac = InternalActionContext.create(rc);
					String roleUuid = ac.getParameter("param0");
					String pathToElement = ac.getParameter("param1");
					crudHandler.handlePermissionUpdate(ac, roleUuid, pathToElement);
				});

		Endpoint permissionGetEndpoint = createEndpoint();
		permissionGetEndpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionRead(ac, roleUuid, pathToElement);
		});
	}

	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.description("Delete the role with the given uuid");
		endpoint.path("/:uuid");
		endpoint.method(DELETE);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.description("Update the role with the given uuid.");
		endpoint.path("/:uuid");
		endpoint.method(PUT);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleRead(ac, uuid);
		});

		/*
		 * List all roles when no parameter was specified
		 */
		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
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
		endpoint.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}
}
