package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.handler.ActionContext;

import io.vertx.ext.web.Route;

@Component
@Scope("singleton")
@SpringVerticle
public class ProjectVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private ProjectCrudHandler crudHandler;

	protected ProjectVerticle() {
		super("projects");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleUpdate(ActionContext.create(rc));
		});
	}

	// TODO when the root tag is not saved the project can't be saved. Unfortunately this did not show up as an http error. We must handle those
	// cases. They must show up in any case.
	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleCreate(ActionContext.create(rc));
		});
	}

	private void addReadHandler() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				crudHandler.handleRead(ActionContext.create(rc));
			}
		});

		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleReadList(ActionContext.create(rc));
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleDelete(ActionContext.create(rc));
		});
	}
}
