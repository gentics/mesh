package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;

import io.vertx.ext.web.Route;

/**
 * Endpoint for /api/v1/languages
 * 
 * NOTE: This endpoint is currently not active
 */
public class LanguageEndpoint extends AbstractProjectEndpoint {

	private final LanguageCrudHandler crudHandler;

	@Inject
	public LanguageEndpoint(MeshAuthChainImpl chain, BootstrapInitializer boot, LocalConfigApi localConfigApi, Database db, MeshOptions options, LanguageCrudHandler crudHandler) {
		super("languages", chain, boot, localConfigApi, db, options);
		this.crudHandler = crudHandler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of languages.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		// TODO Add method that allows assigning languages from and to the project
		Route createRoute = route("/")
				.method(POST)
				.produces(APPLICATION_JSON);
		createRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleCreate(ac);
		}, isOrderedBlockingHandlers());

		Route deleteRoute = route("/:languageUuid")
				.method(DELETE)
				.produces(APPLICATION_JSON);
		deleteRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("languageUuid");
			crudHandler.handleDelete(ac, uuid);
		}, isOrderedBlockingHandlers());

		Route getAllRoute = route("/")
				.method(GET)
				.produces(APPLICATION_JSON);
		getAllRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadList(ac);
		}, false);

		Route getRoute = route("/:languageUuid")
				.method(GET)
				.produces(APPLICATION_JSON);
		getRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("languageUuid");
			crudHandler.handleRead(ac, uuid);
		}, false);

		Route getByTagRoute = route("/tag/:languageTag")
				.method(GET)
				.produces(APPLICATION_JSON);
		getByTagRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tag = ac.getParameter("languageTag");
			crudHandler.handleReadByTag(ac, tag);
		}, false);
	}
}
