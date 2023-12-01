package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

/**
 * Endpoint for /api/v1/languages
 * 
 * NOTE: This endpoint is currently not active
 */
public class LanguageEndpoint extends AbstractInternalEndpoint {

	private final LanguageCrudHandler crudHandler;

	@Inject
	public LanguageEndpoint(MeshAuthChainImpl chain, LocalConfigApi localConfigApi, Database db, MeshOptions options, LanguageCrudHandler crudHandler) {
		super("languages", chain, localConfigApi, db, options);
		this.crudHandler = crudHandler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of languages.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		InternalEndpointRoute createRoute = createRoute();
		createRoute.path("/");
		createRoute.method(POST);
		createRoute.produces(APPLICATION_JSON);
		createRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleCreate(ac);
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute deleteRoute = createRoute();
		deleteRoute.path("/:languageUuid");
		deleteRoute.method(DELETE);
		deleteRoute.produces(APPLICATION_JSON);
		deleteRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("languageUuid");
			crudHandler.handleDelete(ac, uuid);
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute getAllRoute = createRoute();
		getAllRoute.path("/");
		getAllRoute.method(GET);
		getAllRoute.produces(APPLICATION_JSON);
		getAllRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadList(ac);
		}, false);

		InternalEndpointRoute getRoute = createRoute();
		getRoute.path("/:languageUuid");
		getRoute.method(GET);
		getRoute.produces(APPLICATION_JSON);
		getRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("languageUuid");
			crudHandler.handleRead(ac, uuid);
		}, false);

		InternalEndpointRoute getByTagRoute = createRoute();
		getByTagRoute.path("/tag/:languageTag");
		getByTagRoute.method(GET);
		getByTagRoute.produces(APPLICATION_JSON);
		getByTagRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tag = ac.getParameter("languageTag");
			crudHandler.handleReadByTag(ac, tag);
		}, false);
	}
}
