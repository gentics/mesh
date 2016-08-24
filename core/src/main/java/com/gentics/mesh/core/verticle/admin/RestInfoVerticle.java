package com.gentics.mesh.core.verticle.admin;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.MeshNameProvider;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.ext.web.Router;

public class RestInfoVerticle extends AbstractWebVerticle {

	private SearchProvider searchProvider;

	private Database db;

	@Inject
	public RestInfoVerticle(String basePath, Database db, RouterStorage routerStorage, MeshSpringConfiguration springConfig,
			SearchProvider searchProvider) {
		super(null, routerStorage, springConfig);
		this.searchProvider = searchProvider;
		this.db = db;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints that return information about the currently used REST Api";
	}

	@Override
	public void registerEndPoints() throws Exception {
		//Endpoint endpoint = createEndpoint();
		routerStorage.getAPIRouter().route("/").method(GET).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			MeshServerInfoModel info = new MeshServerInfoModel();
			info.setDatabaseVendor(db.getVendorName());
			info.setDatabaseVersion(db.getVersion());
			info.setSearchVendor(searchProvider.getVendorName());
			info.setSearchVersion(searchProvider.getVersion());
			info.setMeshVersion(Mesh.getPlainVersion());
			info.setMeshNodeId(MeshNameProvider.getInstance().getName());
			info.setVertxVersion(new io.vertx.core.Starter().getVersion());
			ac.send(info, OK);
		});
	}

	@Override
	public Router setupLocalRouter() {
		return Router.router(vertx);
	}

}
