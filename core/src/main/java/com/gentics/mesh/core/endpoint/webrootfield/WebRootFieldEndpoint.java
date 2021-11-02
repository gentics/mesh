package com.gentics.mesh.core.endpoint.webrootfield;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;

/**
 * Endpoint for Webroot field functions: '/api/{version}/{project}/webrootfield/{fieldName}/{path}'.
 * 
 * @author plyhun
 *
 */
public class WebRootFieldEndpoint extends AbstractProjectEndpoint {
	
	private WebRootFieldHandler handler;

	public WebRootFieldEndpoint() {
		super("webrootfield", null, null);
	}

	@Inject
	public WebRootFieldEndpoint(MeshAuthChainImpl chain, BootstrapInitializer boot, WebRootFieldHandler handler) {
		super("webrootfield", chain, boot);
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoint which allow viewing the requested field for the node loaded via a webroot path.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addErrorHandlers();
		addGetHandler();
	}

	private void addGetHandler() {
		InternalEndpointRoute fieldGet = createRoute();
		fieldGet.pathRegex("\\/([_a-zA-Z][_a-zA-Z0-9]*)\\/(.*)");
		fieldGet.setRAMLPath("/{fieldName}/{path}");
		fieldGet.addUriParameter("fieldName", "Name of the field which should be acquired.", "stringField");
		fieldGet.addUriParameter("path", "Path to the node", "/News/2015/Images/flower.jpg");
		fieldGet.exampleResponse(OK, "JSON for a node/micronode/list field value, or the binary data for the binary field, or the text data for any other field of the node for the given path.", MeshHeaders.WEBROOT_RESPONSE_TYPE, "node",
				"Header value which identifies the type of the webrootfield response. The response can either be a JSON, text or binary response.");
		fieldGet.description("Load the field content for the node, which is located using the provided path.");
		fieldGet.addQueryParameters(ImageManipulationParametersImpl.class);
		fieldGet.addQueryParameters(VersioningParametersImpl.class);
		fieldGet.method(GET);
		fieldGet.description(
			"Download the field with the given name from the given path. You can use image query parameters for crop and resize if the binary data represents an image.");
		fieldGet.blockingHandler(rc -> {
			handler.handleGetPathField(rc);
		});

	}

	private void addErrorHandlers() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/error/404");
		endpoint.description("Fallback endpoint for unresolvable links which returns 404.");
		endpoint.handler(rc -> {
			rc.data().put("statuscode", "404");
			rc.next();
		});
	}
}
