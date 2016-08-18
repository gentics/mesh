package com.gentics.mesh.core.verticle.admin;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.MeshNameProvider;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.ext.web.Router;

@Component
@Scope(value = "singleton")
@SpringVerticle
public class RestInfoVerticle extends AbstractWebVerticle {

	@Autowired
	private SearchProvider searchProvider;

	protected RestInfoVerticle() {
		super(null);
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
