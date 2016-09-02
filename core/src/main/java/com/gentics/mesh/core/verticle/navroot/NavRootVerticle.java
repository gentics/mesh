package com.gentics.mesh.core.verticle.navroot;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.parameter.impl.NavigationParameters;
import com.gentics.mesh.rest.Endpoint;

@Singleton
public class NavRootVerticle extends AbstractProjectRestVerticle {

	private NavRootHandler handler;

	public NavRootVerticle() {
		super("navroot", null, null);
	}

	@Inject
	public NavRootVerticle(BootstrapInitializer boot, RouterStorage routerStorage, NavRootHandler handler) {
		super("navroot", boot, routerStorage);
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "Provides an endpoint which can be used to retrieve a navigation response";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		addPathHandler();
	}

	private void addPathHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.pathRegex("\\/(.*)");
		endpoint.method(GET);
		endpoint.description("Return a navigation for the node which is located using the given path.");
		endpoint.setRAMLPath("/{path}");
		endpoint.addUriParameter("path", "Webroot path to the node language variation.", "someFolder/somePage.html");
		endpoint.addQueryParameters(NavigationParameters.class);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, nodeExamples.getNavigationResponse(), "Loaded navigation.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String path = ac.getParameter("param0");
			handler.handleGetPath(ac, path);
		});
	}
}
