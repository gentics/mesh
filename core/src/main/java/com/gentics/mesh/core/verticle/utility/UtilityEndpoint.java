package com.gentics.mesh.core.verticle.utility;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.rest.EndpointRoute;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Verticle providing endpoints for various utilities.
 */
@Singleton
public class UtilityEndpoint extends AbstractEndpoint {

	private UtilityHandler utilityHandler;

	@Inject
	public UtilityEndpoint(RouterStorage routerStorage, UtilityHandler utilityHandler) {
		super("utilities", routerStorage);
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
		withBodyHandler();

		addResolveLinkHandler();
		addSchemaValidationHandler();
		addMicroschemaValidationHandler();
	}

	private void addSchemaValidationHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/validateSchema");
		endpoint.method(POST);
		endpoint.description("Validate the posted schema and report errors.");
		endpoint.exampleRequest(schemaExamples.getSchemaUpdateRequest());
		endpoint.exampleResponse(OK, utilityExamples.createValidationResponse(), "The validation message");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			utilityHandler.validateSchema(ac);
		});
	}

	private void addMicroschemaValidationHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/validateMicroschema");
		endpoint.method(POST);
		endpoint.description("Validate the posted microschema and report errors.");
		endpoint.exampleRequest(microschemaExamples.getGeolocationMicroschemaCreateRequest());
		endpoint.exampleResponse(OK, utilityExamples.createValidationResponse(), "The validation report");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			utilityHandler.validateMicroschema(ac);
		});
	}

	/**
	 * Add the handler for link resolving.
	 */
	private void addResolveLinkHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/linkResolver");
		endpoint.method(POST);
		endpoint.description("Return the posted text and resolve and replace all found mesh links. "
				+ "A mesh link must be in the format {{mesh.link(\"UUID\",\"languageTag\")}}");
		endpoint.addQueryParameters(NodeParametersImpl.class);
		endpoint.exampleRequest("Some text before {{mesh.link(\"" + UUIDUtil.randomUUID() + "\", \"en\")}} and after.");
		endpoint.exampleResponse(OK, "Some text before /api/v1/dummy/webroot/flower.jpg and after");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			utilityHandler.handleResolveLinks(ac);
		});
	}
}
