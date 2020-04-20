package com.gentics.mesh.core.endpoint.utility;

import static com.gentics.mesh.example.ExampleUuids.NODE_DELOREAN_UUID;
import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_BASE_PATH;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

/**
 * Verticle providing endpoints for various utilities.
 */
public class UtilityEndpoint extends AbstractInternalEndpoint {

	private UtilityHandler utilityHandler;

	@Inject
	public UtilityEndpoint(MeshAuthChain chain, UtilityHandler utilityHandler) {
		super("utilities", chain);
		this.utilityHandler = utilityHandler;
	}

	public UtilityEndpoint() {
		super("utilities", null);
	}

	@Override
	public String getDescription() {
		return "Provides endpoints for various utility actions";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addResolveLinkHandler();
		addSchemaValidationHandler();
		addMicroschemaValidationHandler();
	}

	private void addSchemaValidationHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/validateSchema");
		endpoint.method(POST);
		endpoint.setMutating(false);
		endpoint.description("Validate the posted schema and report errors.");
		endpoint.exampleRequest(schemaExamples.getSchemaUpdateRequest());
		endpoint.exampleResponse(OK, utilityExamples.createValidationResponse(), "The validation message");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			utilityHandler.validateSchema(ac);
		});
	}

	private void addMicroschemaValidationHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/validateMicroschema");
		endpoint.method(POST);
		endpoint.setMutating(false);
		endpoint.description("Validate the posted microschema and report errors.");
		endpoint.exampleRequest(microschemaExamples.getGeolocationMicroschemaCreateRequest());
		endpoint.exampleResponse(OK, utilityExamples.createValidationResponse(), "The validation report");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			utilityHandler.validateMicroschema(ac);
		});
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
		});
	}
}
