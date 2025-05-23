package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_YAML;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.example.RestInfoExamples;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

/**
 * Endpoint definition for /api/v1 routes.
 */
public class RestInfoEndpoint extends AbstractInternalEndpoint {

	private RestInfoExamples examples = new RestInfoExamples();

	private AdminHandler adminHandler;

	private RouterStorage routerStorage;

	@Inject
	public RestInfoEndpoint(MeshAuthChain chain, AdminHandler adminHandler, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super(null, chain, localConfigApi, db, options);
		this.adminHandler = adminHandler;
	}

	public RestInfoEndpoint(String path) {
		super(path, null, null, null, null);
	}

	@Override
	public void init(Vertx vertx, RouterStorage rs) {
		this.routerStorage = rs;
		localRouter = Router.router(vertx);
	}

	@Override
	public String getDescription() {
		return "Provides endpoints that return information about the REST API.";
	}

	@Override
	public void registerEndPoints() {

		secure("/raml");
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/raml");
		endpoint.method(GET);
		endpoint.description("Endpoint which provides a RAML document for all registed endpoints.");
		endpoint.displayName("RAML specification");
		endpoint.exampleResponse(OK, "Not yet specified");
		endpoint.produces(APPLICATION_YAML);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			adminHandler.handleRAML(ac);
		}, false);

		secure("/");
		InternalEndpointRoute infoEndpoint = createRoute();
		infoEndpoint.path("/");
		infoEndpoint.description("Endpoint which returns version information");
		infoEndpoint.displayName("Version Information");
		infoEndpoint.produces(APPLICATION_JSON);
		infoEndpoint.exampleResponse(OK, examples.getInfoExample(), "JSON which contains version information");
		infoEndpoint.method(GET);
		infoEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			adminHandler.handleVersions(ac);
		}, false);
	}

	@Override
	public Router getRouter() {
		return routerStorage.root().apiRouter().getRouter();
	}

}
