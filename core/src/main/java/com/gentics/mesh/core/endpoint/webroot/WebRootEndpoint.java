package com.gentics.mesh.core.endpoint.webroot;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;

public class WebRootEndpoint extends AbstractProjectEndpoint {

	private WebRootHandler handler;

	public WebRootEndpoint() {
		super("webroot", null);
	}

	@Inject
	public WebRootEndpoint(BootstrapInitializer boot, WebRootHandler handler) {
		super("webroot", boot);
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow loading nodes via a webroot path.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addErrorHandlers();
		addPathHandler();
	}

	private void addPathHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.pathRegex("\\/(.*)");
		endpoint.setRAMLPath("/{path}");
		endpoint.method(GET);
		endpoint.addUriParameter("path", "Path to the node", "/News/2015/Images/flower.jpg");
		endpoint.exampleResponse(OK, "JSON for a node or the binary data of the node for the given path.", MeshHeaders.WEBROOT_RESPONSE_TYPE, "node",
				"Header value which identifies the type of the webroot response. The response can either be a node or binary response.");
		endpoint.description("Load the node or the node's binary data which is located using the provided path.");
		endpoint.addQueryParameters(ImageManipulationParametersImpl.class);
		endpoint.handler(rc -> {
			handler.handleGetPath(rc);
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
