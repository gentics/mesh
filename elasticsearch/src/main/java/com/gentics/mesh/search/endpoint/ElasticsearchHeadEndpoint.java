package com.gentics.mesh.search.endpoint;

import static io.vertx.core.http.HttpMethod.GET;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class ElasticsearchHeadEndpoint extends AbstractInternalEndpoint {

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
		rs.root().getRouter().mountSubRouter("/" + basePath, router);
		this.routerStorage = rs;
		this.localRouter = router;
	}

	@Override
	public void registerEndPoints() {
		addRedirectionHandler();
		addStaticHandler();
	}

	private void addRedirectionHandler() {
		routerStorage.root().getRouter().route("/").method(GET).handler(rc -> {
			rc.response().setStatusCode(302);
			rc.response().headers().set("Location", "/" + basePath + "/");
			rc.response().end();
		});
		route().method(GET).handler(rc -> {
			if ("/elastichead".equals(rc.request().path())) {
				rc.response().setStatusCode(302);
				rc.response().headers().set("Location", "/" + basePath + "/");
				rc.response().end();
			} else {
				rc.next();
			}
		});
	}

	public void addStaticHandler() {
		StaticHandler staticHandler = StaticHandler.create("elastichead");
		staticHandler.setDirectoryListing(false);
		staticHandler.setCachingEnabled(false);
		staticHandler.setIndexPage("index.html");
		route("/*").method(GET).handler(staticHandler);
	}

}
