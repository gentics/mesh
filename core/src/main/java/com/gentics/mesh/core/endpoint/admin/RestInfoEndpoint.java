package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_YAML;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_YAML_UTF8;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.example.RestInfoExamples;
import com.gentics.mesh.generator.RAMLGenerator;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.impl.launcher.commands.VersionCommand;
import io.vertx.ext.web.Router;

public class RestInfoEndpoint extends AbstractInternalEndpoint {

	private SearchProvider searchProvider;

	private RestInfoExamples examples = new RestInfoExamples();

	private LegacyDatabase db;

	private RouterStorage routerStorage;

	@Inject
	public RestInfoEndpoint(MeshAuthChain chain, LegacyDatabase db, SearchProvider searchProvider) {
		super(null, chain);
		this.searchProvider = searchProvider;
		this.db = db;
	}

	public RestInfoEndpoint(String path) {
		super(path, null);
	}

	@Override
	public void init(RouterStorage rs) {
		this.routerStorage = rs;
		localRouter = Router.router(Mesh.vertx());
	}

	@Override
	public String getDescription() {
		return "Provides endpoints that return information about the REST API.";
	}

	@Override
	public void registerEndPoints() {

		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/raml");
		endpoint.method(GET);
		endpoint.description("Endpoint which provides a RAML document for all registed endpoints.");
		endpoint.displayName("RAML specification");
		endpoint.exampleResponse(OK, "Not yet specified");
		endpoint.produces(APPLICATION_YAML);
		endpoint.blockingHandler(rc -> {
			RAMLGenerator generator = new RAMLGenerator();
			String raml = generator.generate();
			rc.response().putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_YAML_UTF8);
			rc.response().end(raml);
		}, false);

		InternalEndpointRoute infoEndpoint = createRoute();
		infoEndpoint.path("/");
		infoEndpoint.description("Endpoint which returns version information");
		infoEndpoint.displayName("Version Information");
		infoEndpoint.produces(APPLICATION_JSON);
		infoEndpoint.exampleResponse(OK, examples.getInfoExample(), "JSON which contains version information");
		infoEndpoint.method(GET);
		infoEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			MeshServerInfoModel info = new MeshServerInfoModel();
			info.setDatabaseVendor(db.getVendorName());
			info.setDatabaseVersion(db.getVersion());
			info.setSearchVendor(searchProvider.getVendorName());
			info.setSearchVersion(searchProvider.getVersion());
			info.setMeshVersion(Mesh.getPlainVersion());
			info.setMeshNodeName(Mesh.mesh().getOptions().getNodeName());
			info.setVertxVersion(VersionCommand.getVersion());
			info.setDatabaseRevision(db.getDatabaseRevision());
			ac.send(info, OK);
		}, false);
	}

	@Override
	public Router getRouter() {
		return routerStorage.root().apiRouter().getRouter();
	}

}
