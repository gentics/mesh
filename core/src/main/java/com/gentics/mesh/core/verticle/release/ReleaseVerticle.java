package com.gentics.mesh.core.verticle.release;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.POST;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.ext.web.Route;

/**
 * Verticle for REST endpoints to manage Releases
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ReleaseVerticle extends AbstractProjectRestVerticle {
	@Autowired
	private ReleaseCrudHandler crudHandler;

	public ReleaseVerticle() {
		super("releases");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());

		addCreateHandler();
	}

	private void addCreateHandler() {
		Route route = route("/").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> crudHandler.handleCreate(InternalActionContext.create(rc)));
	}
}
