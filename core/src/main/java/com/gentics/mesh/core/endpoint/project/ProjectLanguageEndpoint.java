package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
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
import com.gentics.mesh.example.ExampleUuids;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.ProjectLoadParametersImpl;
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
		getAllRoute.description("Get all languages, assigned to this project");
		getAllRoute.exampleResponse(OK, languageExamples.getLanguageListResponse(), "List of languages");
		getAllRoute.addQueryParameters(GenericParametersImpl.class);
		getAllRoute.addQueryParameters(PagingParametersImpl.class);
		getAllRoute.method(GET);
		getAllRoute.produces(APPLICATION_JSON);
		getAllRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadList(ac);
		}, false);

		InternalEndpointRoute getRoute = createRoute();
		getRoute.path("/:languageUuid");
		getRoute.addUriParameter("languageUuid", "UUID of a language", ExampleUuids.UUID_1);
		getRoute.addQueryParameters(GenericParametersImpl.class);
		getRoute.exampleResponse(OK, languageExamples.getJapaneseLanguageResponse(), "A language");
		getRoute.method(GET);
		getRoute.produces(APPLICATION_JSON);
		getRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("languageUuid");
			crudHandler.handleRead(ac, uuid);
		}, false);

		InternalEndpointRoute getByTagRoute = createRoute();
		getByTagRoute.path("/tag/:languageTag");
		getByTagRoute.addUriParameter("languageTag", "ISO language tag", "jp");
		getByTagRoute.addQueryParameters(GenericParametersImpl.class);
		getByTagRoute.exampleResponse(OK, languageExamples.getJapaneseLanguageResponse(), "A language");
		getByTagRoute.method(GET);
		getByTagRoute.produces(APPLICATION_JSON);
		getByTagRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tag = ac.getParameter("languageTag");
			crudHandler.handleReadByTag(ac, tag);
		}, false);

		InternalEndpointRoute assignRoute = createRoute();
		assignRoute.path("/:languageUuid");
		assignRoute.addUriParameter("languageUuid", "UUID of a language to assign", ExampleUuids.UUID_1);
		assignRoute.addQueryParameters(GenericParametersImpl.class);
		assignRoute.addQueryParameters(ProjectLoadParametersImpl.class);
		assignRoute.exampleResponse(OK, projectExamples.getProjectResponseWithLanguages("Multilingual_project"), "A project");
		assignRoute.method(POST);
		assignRoute.produces(APPLICATION_JSON);
		assignRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("languageUuid");
			crudHandler.handleAssignLanguageToProject(ac, uuid, "languageUuid");
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute unassignRoute = createRoute();
		unassignRoute.path("/:languageUuid");
		unassignRoute.addUriParameter("languageUuid", "UUID of a language to unassign", ExampleUuids.UUID_1);
		unassignRoute.addQueryParameters(GenericParametersImpl.class);
		unassignRoute.addQueryParameters(ProjectLoadParametersImpl.class);
		unassignRoute.exampleResponse(OK, projectExamples.getProjectResponseWithLanguages("Multilingual_project"), "A project");
		unassignRoute.method(DELETE);
		unassignRoute.produces(APPLICATION_JSON);
		unassignRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("languageUuid");
			crudHandler.handleUnassignLanguageFromProject(ac, uuid, "languageUuid");
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute assignByTagRoute = createRoute();
		assignByTagRoute.path("/tag/:languageTag");
		assignByTagRoute.addUriParameter("languageTag", "ISO tag of a language to assign", ExampleUuids.UUID_1);
		assignByTagRoute.addQueryParameters(GenericParametersImpl.class);
		assignByTagRoute.addQueryParameters(ProjectLoadParametersImpl.class);
		assignByTagRoute.exampleResponse(OK, projectExamples.getProjectResponseWithLanguages("Multilingual_project"), "A project");
		assignByTagRoute.method(POST);
		assignByTagRoute.produces(APPLICATION_JSON);
		assignByTagRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tag = ac.getParameter("languageTag");
			crudHandler.handleAssignLanguageToProject(ac, tag, "languageTag");
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute unassignByTagRoute = createRoute();
		unassignByTagRoute.path("/tag/:languageTag");
		unassignByTagRoute.addUriParameter("languageTag", "ISO tag of a language to unassign", ExampleUuids.UUID_1);
		unassignByTagRoute.addQueryParameters(GenericParametersImpl.class);
		unassignByTagRoute.addQueryParameters(ProjectLoadParametersImpl.class);
		unassignByTagRoute.exampleResponse(OK, projectExamples.getProjectResponseWithLanguages("Multilingual_project"), "A project");
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
