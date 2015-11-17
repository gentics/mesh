package com.gentics.mesh.core.verticle.role;

import static com.gentics.mesh.core.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.handler.InternalActionContext;

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
		route("/*").handler(springConfiguration.authHandler());
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();

		addPermissionHandler();
	}

	private void addPermissionHandler() {
		localRouter.routeWithRegex("\\/([^\\/]*)\\/permissions\\/(.*)").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON)
				.handler(rc -> {
					crudHandler.handlePermissionUpdate(InternalActionContext.create(rc));
				});

		localRouter.routeWithRegex("\\/([^\\/]*)\\/permissions\\/(.*)").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handlePermissionRead(InternalActionContext.create(rc));
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			crudHandler.handleDelete(InternalActionContext.create(rc));
		});
	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleUpdate(InternalActionContext.create(rc));
		});
	}

	private void addReadHandler() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleRead(InternalActionContext.create(rc));
		});

		/*
		 * List all roles when no parameter was specified
		 */
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addCreateHandler() {
		route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}
}
