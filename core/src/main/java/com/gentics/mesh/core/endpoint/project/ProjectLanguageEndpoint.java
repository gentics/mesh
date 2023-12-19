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
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;

/**
 * Project languages manipulation endpoint.
 */
public class ProjectLanguageEndpoint extends AbstractProjectEndpoint {

	private LanguageCrudHandler crudHandler;

	public ProjectLanguageEndpoint() {
		super("languages", null, null, null, null, null);
	}

	@Inject
	public ProjectLanguageEndpoint(MeshAuthChainImpl chain, BootstrapInitializer boot, LocalConfigApi localConfigApi, Database db, MeshOptions options, LanguageCrudHandler crudHandler) {
		super("languages", chain, boot, localConfigApi, db, options);
		this.crudHandler = crudHandler;
	}

	@Override
	public void registerEndPoints() {
		secureAll();

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

		InternalEndpointRoute assignRoute = createRoute();
		assignRoute.path("/:languageUuid");
		assignRoute.method(POST);
		assignRoute.produces(APPLICATION_JSON);
		assignRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("languageUuid");
			crudHandler.handleAssignLanguageToProject(ac, uuid, "languageUuid");
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute unassignRoute = createRoute();
		unassignRoute.path("/:languageUuid");
		unassignRoute.method(DELETE);
		unassignRoute.produces(APPLICATION_JSON);
		unassignRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("languageUuid");
			crudHandler.handleUnassignLanguageFromProject(ac, uuid, "languageUuid");
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute assignByTagRoute = createRoute();
		assignByTagRoute.path("/tag/:languageTag");
		assignByTagRoute.method(POST);
		assignByTagRoute.produces(APPLICATION_JSON);
		assignByTagRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tag = ac.getParameter("languageTag");
			crudHandler.handleAssignLanguageToProject(ac, tag, "languageTag");
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute unassignByTagRoute = createRoute();
		unassignByTagRoute.path("/tag/:languageTag");
		unassignByTagRoute.method(DELETE);
		unassignByTagRoute.produces(APPLICATION_JSON);
		unassignByTagRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tag = ac.getParameter("languageTag");
			crudHandler.handleUnassignLanguageFromProject(ac, tag, "languageTag");
		}, isOrderedBlockingHandlers());
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of project languages.";
	}
}
