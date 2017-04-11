package com.gentics.mesh.search.verticle;

import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.etc.RouterStorage;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

@Singleton
public class ElasticsearchHeadVerticle extends AbstractWebVerticle {

	@Inject
	public ElasticsearchHeadVerticle(RouterStorage routerStorage) {
		super("elastichead", routerStorage);
	}

	@Override
	public void registerEndPoints() throws Exception {
		addStaticHandler();
	}

	public void addStaticHandler() {
		StaticHandler staticHandler = StaticHandler.create("elastichead");
		staticHandler.setDirectoryListing(false);
		staticHandler.setCachingEnabled(false);
		staticHandler.setIndexPage("index.html");
		route("/*").method(GET).handler(staticHandler);
	}

	@Override
	public Router setupLocalRouter() {
		Router router = Router.router(vertx);
		routerStorage.getRootRouter().mountSubRouter("/" + basePath, router);
		return router;
	}

}
