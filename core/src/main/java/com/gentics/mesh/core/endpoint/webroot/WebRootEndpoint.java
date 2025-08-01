package com.gentics.mesh.core.endpoint.webroot;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;

/**
 * REST API endpoint definition for /api/v1/:project/webroot
 */
public class WebRootEndpoint extends AbstractProjectEndpoint {

	private WebRootHandler handler;

	public WebRootEndpoint() {
		super("webroot", null, null, null, null, null);
	}

	@Inject
	public WebRootEndpoint(MeshAuthChain chain, BootstrapInitializer boot, WebRootHandler handler, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super("webroot", chain, boot, localConfigApi, db, options);
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
		addPathReadHandler();
		addPathUpdateCreateHandler();
	}

	private void addPathReadHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.pathRegex("\\/(.*)");
		endpoint.setRAMLPath("/{path}");
		endpoint.method(GET);
		endpoint.addUriParameter("path", "Path to the node", "/News/2015/Images/flower.jpg");
		endpoint.exampleResponse(OK, "JSON for a node or the binary data of the node for the given path.", MeshHeaders.WEBROOT_RESPONSE_TYPE, "node",
			"Header value which identifies the type of the webroot response. The response can either be a node or binary response.");
		endpoint.description("Load the node or the node's binary data which is located using the provided path.");
		endpoint.addQueryParameters(ImageManipulationParametersImpl.class);
		endpoint.addQueryParameters(VersioningParametersImpl.class);
		endpoint.blockingHandler(rc -> {
			handler.handleGetPath(rc);
		}, false);
	}

	private void addPathUpdateCreateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.pathRegex("\\/(.*)");
		endpoint.setRAMLPath("/{path}");
		endpoint.addUriParameter("path", "Path to the node", "/News/2015/Images/flower.jpg");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);

		endpoint.exampleRequest(nodeExamples.getNodeUpdateRequest());
		endpoint.exampleResponse(OK, nodeExamples.getNodeResponse2(), "Updated node.");
		endpoint.exampleResponse(CREATED, nodeExamples.getNodeResponse2(), "Created node.");
		endpoint.exampleResponse(CONFLICT, miscExamples.createMessageResponse(), "A conflict has been detected.");

		endpoint.description("Update or create a node for the given path.");
		endpoint.blockingHandler(rc -> {
			handler.handleUpdateCreatePath(rc, POST);
		}, isOrderedBlockingHandlers());
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
