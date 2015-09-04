package com.gentics.mesh.core.verticle.tagfamily;

import static com.gentics.mesh.core.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.handler.ActionContext;

import io.vertx.ext.web.Route;

@Component
@Scope("singleton")
@SpringVerticle
public class TagFamilyVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private TagFamilyCrudHandler crudHandler;

	public TagFamilyVerticle() {
		super("tagFamilies");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addReadTagsHandler();
		addReadHandler();
		addCreateHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addReadTagsHandler() {
		Route route = route("/:tagFamilyUuid/tags").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleReadTagList(ActionContext.create(rc));
		});
	}

	private void addDeleteHandler() {
		Route deleteRoute = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			crudHandler.handleDelete(ActionContext.create(rc));
		});
	}

	private void addReadHandler() {
		Route readRoute = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		readRoute.handler(rc -> {
			crudHandler.handleRead(ActionContext.create(rc));
		});

		Route readAllRoute = route().method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			crudHandler.handleReadList(ActionContext.create(rc));
		});
	}

	private void addCreateHandler() {
		Route createRoute = route().method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		createRoute.handler(rc -> {
			crudHandler.handleCreate(ActionContext.create(rc));
		});
	}

	private void addUpdateHandler() {
		Route updateRoute = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		updateRoute.handler(rc -> {
			crudHandler.handleUpdate(ActionContext.create(rc));
		});
	}
}
