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
import com.gentics.mesh.handler.ActionContext;

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
		localRouter.routeWithRegex("\\/([^\\/]*)\\/permissions\\/(.*)").method(PUT).handler(rc -> {
			crudHandler.handlePermissionUpdate(ActionContext.create(rc));
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			crudHandler.handleDelete(ActionContext.create(rc));
		});
	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleUpdate(ActionContext.create(rc));
		});
	}

	private void addReadHandler() {
		route("/:uuid").method(GET).handler(rc -> {
			crudHandler.handleRead(ActionContext.create(rc));
		});

		/*
		 * List all roles when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {
			crudHandler.handleReadList(ActionContext.create(rc));
		});
	}

	private void addCreateHandler() {
		route("/").method(POST).consumes(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleCreate(ActionContext.create(rc));
		});
	}
}
