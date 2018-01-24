package com.gentics.mesh.search.endpoint;

import static io.vertx.core.http.HttpMethod.GET;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.route.AbstractEndpoint;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class ElasticsearchHeadEndpoint extends AbstractEndpoint {

	public ElasticsearchHeadEndpoint() {
		super("elastichead");
	}

	@Override
	public String getDescription() {
		return "Endpoint which provides the elastichead webapp";
	}

	@Override
	public void init(RouterStorage rs) {
		Router router = Router.router(Mesh.vertx());
		rs.getRootRouter().mountSubRouter("/" + basePath, router);
		this.routerStorage = rs;
		this.localRouter = router;
	}

	@Override
	public void registerEndPoints() {
		addStaticHandler();
	}

	public void addStaticHandler() {
		StaticHandler staticHandler = StaticHandler.create("elastichead");
		staticHandler.setDirectoryListing(false);
		staticHandler.setCachingEnabled(false);
		staticHandler.setIndexPage("index.html");
		route("/*").method(GET).handler(staticHandler);
	}

}
