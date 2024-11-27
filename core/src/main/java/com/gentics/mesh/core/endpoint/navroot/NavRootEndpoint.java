package com.gentics.mesh.core.endpoint.navroot;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.impl.NavigationParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;

/**
 * Endpoint which returns navigation responses for a given webroot path.
 */	
public class NavRootEndpoint extends AbstractProjectEndpoint {

	private NavRootHandler handler;

	public NavRootEndpoint() {
		super("navroot", null, null, null, null, null);
	}

	@Inject
	public NavRootEndpoint(MeshAuthChain chain, BootstrapInitializer boot, NavRootHandler handler, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super("navroot", chain, boot, localConfigApi, db, options);
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "Provides an endpoint which can be used to retrieve a navigation response";
	}

	@Override
	public void registerEndPoints() {
		secureAll();
		addPathHandler();
	}

	private void addPathHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.pathRegex("\\/(.*)");
		endpoint.method(GET);
		endpoint.description("Return a navigation for the node which is located using the given path.");
		endpoint.setRAMLPath("/{path}");
		endpoint.addUriParameter("path", "Webroot path to the node language variation.", "someFolder/somePage.html");
		endpoint.addQueryParameters(NavigationParametersImpl.class);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, nodeExamples.getNavigationResponse(), "Loaded navigation.");
		endpoint.blockingHandler(rc -> handler.handleGetPath(rc), false);
	}
}
