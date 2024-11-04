package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.example.ExampleUuids;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.InternalCommonEndpoint;
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
	public LanguageEndpoint(MeshAuthChain chain, LocalConfigApi localConfigApi, Database db, MeshOptions options, LanguageCrudHandler crudHandler) {
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

// Unused, due to the 
//		InternalEndpointRoute createRoute = createRoute();
//		createRoute.description("Create a new language. Currently unused, throwing an error.");
//		createRoute.exampleResponse(OK, languageExamples.getGermanLanguageResponse(), "A language");
//		createRoute.path("/");
//		createRoute.method(POST);
//		createRoute.produces(APPLICATION_JSON);
//		createRoute.blockingHandler(rc -> {
//			InternalActionContext ac = wrap(rc);
//			crudHandler.handleCreate(ac);
//		}, isOrderedBlockingHandlers());
//
//		InternalEndpointRoute deleteRoute = createRoute();
//		deleteRoute.description("Delete the language. Currently unused, throwing an error.");
//		deleteRoute.exampleResponse(OK, languageExamples.getGermanLanguageResponse(), "A language");
//		deleteRoute.path("/:languageUuid");
//		deleteRoute.method(DELETE);
//		deleteRoute.produces(APPLICATION_JSON);
//		deleteRoute.blockingHandler(rc -> {
//			InternalActionContext ac = wrap(rc);
//			String uuid = ac.getParameter("languageUuid");
//			crudHandler.handleDelete(ac, uuid);
//		}, isOrderedBlockingHandlers());

		InternalEndpointRoute getAllRoute = createRoute();
		getAllRoute.path("/");
		getAllRoute.description("Get all system installed languages");
		getAllRoute.exampleResponse(OK, InternalCommonEndpoint.languageExamples.getLanguageListResponse(), "List of languages");
		getAllRoute.addQueryParameters(PagingParametersImpl.class);
		getAllRoute.method(GET);
		getAllRoute.produces(APPLICATION_JSON);
		getAllRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadList(ac);
		}, false);

		InternalEndpointRoute getRoute = createRoute();
		getRoute.path("/:languageUuid");
		getRoute.description("Get a system installed language by its UUID");
		getRoute.addUriParameter("languageUuid", "UUID of a language", ExampleUuids.UUID_1);
		getRoute.exampleResponse(OK, InternalCommonEndpoint.languageExamples.getJapaneseLanguageResponse(), "A language");
		getRoute.method(GET);
		getRoute.produces(APPLICATION_JSON);
		getRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("languageUuid");
			crudHandler.handleRead(ac, uuid);
		}, false);

		InternalEndpointRoute getByTagRoute = createRoute();
		getByTagRoute.path("/tag/:languageTag");
		getByTagRoute.description("Get a system installed language by its ISO tag");
		getByTagRoute.addUriParameter("languageTag", "ISO language tag", "jp");
		getByTagRoute.exampleResponse(OK, InternalCommonEndpoint.languageExamples.getJapaneseLanguageResponse(), "A language");
		getByTagRoute.method(GET);
		getByTagRoute.produces(APPLICATION_JSON);
		getByTagRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tag = ac.getParameter("languageTag");
			crudHandler.handleReadByTag(ac, tag);
		}, false);
	}
}
