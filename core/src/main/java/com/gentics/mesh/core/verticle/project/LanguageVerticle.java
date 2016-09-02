package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.etc.RouterStorage;

import io.vertx.ext.web.Route;

public class LanguageVerticle extends AbstractProjectRestVerticle {

	@Inject
	public LanguageVerticle(BootstrapInitializer boot, RouterStorage routerStorage) {
		super("languages", boot, routerStorage);
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of languages.";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();

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
