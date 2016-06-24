package com.gentics.mesh.core.verticle.user;

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
public class UserVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private UserCrudHandler crudHandler;

	public UserVerticle() {
		super("users");
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

		addReadPermissionHandler();
	}

	private void addReadPermissionHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String userUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionRead(ac, userUuid, pathToElement);
		});
	}

	private void addReadHandler() {
		Endpoint readEndpoint = createEndpoint();
		readEndpoint.path("/:uuid");
		readEndpoint.method(GET);
		readEndpoint.produces(APPLICATION_JSON);
		readEndpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleRead(ac, uuid);
		});

		/*
		 * List all users when no parameter was specified
		 */
		Endpoint readAllEndpoint = createEndpoint();
		readAllEndpoint.path("/");
		readAllEndpoint.method(GET);
		readAllEndpoint.produces(APPLICATION_JSON);
		readAllEndpoint.handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
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
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addCreateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}
}
