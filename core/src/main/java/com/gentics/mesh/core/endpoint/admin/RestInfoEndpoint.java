package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_YAML;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.context.InternalActionContext;
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
	public RestInfoEndpoint(MeshAuthChainImpl chain, AdminHandler adminHandler) {
		super(null, chain);
		this.adminHandler = adminHandler;
	}

	public RestInfoEndpoint(String path) {
		super(path, null);
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

		secure("/openapi.yaml");
		InternalEndpointRoute openapiYml = createRoute();
		openapiYml.path("/openapi.yaml");
		openapiYml.method(GET);
		openapiYml.description("Endpoint which provides a OpenAPIv3 YAML document for all registed endpoints.");
		openapiYml.displayName("OpenAPI YAML specification");
		openapiYml.exampleResponse(OK, "Not yet specified");
		openapiYml.produces(APPLICATION_YAML);
		openapiYml.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			adminHandler.handleOpenAPIv3(ac, "yaml");
		}, false);

		secure("/openapi.json");
		InternalEndpointRoute openapiJson = createRoute();
		openapiJson.path("/openapi.json");
		openapiJson.method(GET);
		openapiJson.description("Endpoint which provides a OpenAPIv3 JSON document for all registed endpoints.");
		openapiJson.displayName("OpenAPI JSON specification");
		openapiJson.exampleResponse(OK, "Not yet specified");
		openapiJson.produces(APPLICATION_JSON);
		openapiJson.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			adminHandler.handleOpenAPIv3(ac, "json");
		}, false);

		secure("/");
		InternalEndpointRoute infoEndpoint = createRoute();
		infoEndpoint.setInsecure(true);
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
