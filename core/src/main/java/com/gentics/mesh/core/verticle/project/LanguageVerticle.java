package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import org.apache.commons.lang3.NotImplementedException;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;

import io.vertx.ext.web.Route;

@Component
@Scope("singleton")
@SpringVerticle
public class LanguageVerticle extends AbstractProjectRestVerticle {

	protected LanguageVerticle() {
		super("languages");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());

		// TODO Add method that allows assigning languages from and to the project
		Route createRoute = route("/:projectUuid/languages").method(POST).produces(APPLICATION_JSON);
		createRoute.handler(rc -> {
			throw new NotImplementedException("not implemented");
		});

		Route deleteRoute = route("/:projectUuid/languages").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			// Unassign languages should cause a batch process that removes the FieldContainers for the given language.
			throw new NotImplementedException("not implemented");
		});

		Route getRoute = route("/:projectUuid/languages").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			throw new NotImplementedException("not implemented");
		});
	}

}
