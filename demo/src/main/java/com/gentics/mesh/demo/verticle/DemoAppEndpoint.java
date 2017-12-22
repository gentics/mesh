package com.gentics.mesh.demo.verticle;

import static io.vertx.core.http.HttpMethod.GET;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.route.AbstractEndpoint;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Endpoint which provides the demo angular webapp.
 */
public class DemoAppEndpoint extends AbstractEndpoint {

	public DemoAppEndpoint() {
		super("demo");
	}

	@Override
	public String getDescription() {
		return "Endpoint which provides the demo angular application";
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
		addRedirectionHandler();

		StaticHandler staticHandler = StaticHandler.create("demo");
		staticHandler.setDirectoryListing(false);
		staticHandler.setCachingEnabled(false);
		staticHandler.setIndexPage("index.html");
		route("/*").method(GET).handler(staticHandler);
	}

	private void addRedirectionHandler() {
		route().method(GET).handler(rc -> {
			if ("/demo".equals(rc.request().path())) {
				rc.response().setStatusCode(302);
				rc.response().headers().set("Location", "/" + basePath + "/");
				rc.response().end();
			} else {
				rc.next();
			}
		});
	}
}
