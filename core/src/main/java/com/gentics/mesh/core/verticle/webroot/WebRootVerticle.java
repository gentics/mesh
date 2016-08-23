package com.gentics.mesh.core.verticle.webroot;

import static io.vertx.core.http.HttpMethod.GET;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.parameter.impl.ImageManipulationParameters;
import com.gentics.mesh.rest.Endpoint;

public class WebRootVerticle extends AbstractProjectRestVerticle {

	private WebRootHandler handler;

	public WebRootVerticle(WebRootHandler handler) {
		super("webroot");
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow loading nodes via a webroot path.";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		addErrorHandlers();
		addPathHandler();
	}

	private void addPathHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.pathRegex("\\/(.*)");
		endpoint.setRAMLPath("/{path}");
		endpoint.method(GET);
		endpoint.addUriParameter("path", "Path to the node", "/News/2015/Images/flower.jpg");
		endpoint.description("Load the node or the node's binary data which is located using the provided path.");
		endpoint.addQueryParameters(ImageManipulationParameters.class);
		endpoint.handler(rc -> {
			handler.handleGetPath(rc);
		});
	}

	private void addErrorHandlers() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/error/404");
		endpoint.description("Fallback endpoint for unresolvable links which returns 404.");
		endpoint.handler(rc -> {
			rc.data().put("statuscode", "404");
			rc.next();
		});
	}
}
