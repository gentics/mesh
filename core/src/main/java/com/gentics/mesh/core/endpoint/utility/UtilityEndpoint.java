package com.gentics.mesh.core.endpoint.utility;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.example.ExampleUuids.NODE_DELOREAN_UUID;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

/**
 * Verticle providing endpoints for various utilities.
 */
public class UtilityEndpoint extends AbstractInternalEndpoint {

	private UtilityHandler utilityHandler;

	@Inject
	public UtilityEndpoint(MeshAuthChain chain, UtilityHandler utilityHandler, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super("utilities", chain, localConfigApi, db, options);
		this.utilityHandler = utilityHandler;
	}

	public UtilityEndpoint() {
		super("utilities", null, null, null, null);
	}

	@Override
	public String getDescription() {
		return "Provides endpoints for various utility actions";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addResolveLinkHandler();
		addValidationHandler();
		addVersionPurgeHandler();
	}

	private void addVersionPurgeHandler() {
		InternalEndpointRoute purgeSchemaVersionsEndpoint = createRoute();
		purgeSchemaVersionsEndpoint.path("/purge/schema/versions");
		purgeSchemaVersionsEndpoint.method(POST);
		purgeSchemaVersionsEndpoint.description("Purge the unused schema versions.");
		purgeSchemaVersionsEndpoint.consumes(APPLICATION_JSON);
		purgeSchemaVersionsEndpoint.exampleRequest(miscExamples.createNameOrUuidsRequest());
		purgeSchemaVersionsEndpoint.produces(APPLICATION_JSON);
		purgeSchemaVersionsEndpoint.exampleResponse(OK, "Schema version purge job initialized.");
		purgeSchemaVersionsEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			utilityHandler.handleSchemaVersionPurge(ac);
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute purgeMicroschemaVersionsEndpoint = createRoute();
		purgeMicroschemaVersionsEndpoint.path("/purge/microschema/versions");
		purgeMicroschemaVersionsEndpoint.method(POST);
		purgeMicroschemaVersionsEndpoint.description("Purge the unused microschema versions.");
		purgeMicroschemaVersionsEndpoint.consumes(APPLICATION_JSON);
		purgeMicroschemaVersionsEndpoint.exampleRequest(miscExamples.createNameOrUuidsRequest());
		purgeMicroschemaVersionsEndpoint.produces(APPLICATION_JSON);
		purgeMicroschemaVersionsEndpoint.exampleResponse(OK, "Microschema version purge job initialized.");
		purgeMicroschemaVersionsEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			utilityHandler.handleMicroschemaVersionPurge(ac);
		}, isOrderedBlockingHandlers());
	}

	private void addValidationHandler() {
		InternalEndpointRoute schemaValidationEndpoint = createRoute();
		schemaValidationEndpoint.path("/validateSchema");
		schemaValidationEndpoint.method(POST);
		schemaValidationEndpoint.setMutating(false);
		schemaValidationEndpoint.description("Validate the posted schema and report errors.");
		schemaValidationEndpoint.exampleRequest(schemaExamples.getSchemaUpdateRequest());
		schemaValidationEndpoint.exampleResponse(OK, utilityExamples.createValidationResponse(), "The validation message");
		schemaValidationEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			utilityHandler.validateSchema(ac);
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute microschemaValidationEndpoint = createRoute();
		microschemaValidationEndpoint.path("/validateMicroschema");
		microschemaValidationEndpoint.method(POST);
		microschemaValidationEndpoint.setMutating(false);
		microschemaValidationEndpoint.description("Validate the posted microschema and report errors.");
		microschemaValidationEndpoint.exampleRequest(microschemaExamples.getGeolocationMicroschemaCreateRequest());
		microschemaValidationEndpoint.exampleResponse(OK, utilityExamples.createValidationResponse(), "The validation report");
		microschemaValidationEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			utilityHandler.validateMicroschema(ac);
		}, false);
	}

	/**
	 * Add the handler for link resolving.
	 */
	private void addResolveLinkHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/linkResolver");
		endpoint.method(POST);
		endpoint.setMutating(false);
		endpoint.description("Return the posted text and resolve and replace all found mesh links. "
			+ "A mesh link must be in the format {{mesh.link(\"UUID\",\"languageTag\")}}");
		endpoint.addQueryParameters(NodeParametersImpl.class);
		endpoint.exampleRequest("Some text before {{mesh.link(\"" + NODE_DELOREAN_UUID + "\", \"en\")}} and after.");
		endpoint.exampleResponse(OK, "Some text before " + CURRENT_API_BASE_PATH + "/dummy/webroot/flower.jpg and after");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			utilityHandler.handleResolveLinks(ac);
		}, false);
	}
}
