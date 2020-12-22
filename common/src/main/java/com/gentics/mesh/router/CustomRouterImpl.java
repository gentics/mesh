package com.gentics.mesh.router;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * The custom router is used to manage custom routes outside of the regular API routes. The basepath for these routes would be /custom/
 */
public class CustomRouterImpl implements CustomRouter {

	private static final Logger log = LoggerFactory.getLogger(CustomRouterImpl.class);

	public static final String CUSTOM_MOUNTPOINT = "/custom";

	private final Router router;

	/**
	 * Custom routers. (E.g.: /demo)
	 */
	private Map<String, Router> customRouters = new HashMap<>();

	private final Vertx vertx;

	public CustomRouterImpl(Vertx vertx, RootRouterImpl root) {
		this.vertx = vertx;
		Router rootRouter = root.getRouter();
		router = Router.router(vertx);
		rootRouter.mountSubRouter(CUSTOM_MOUNTPOINT, router);
	}

	/**
	 * Return the router which mounts custom routers / routes.
	 * 
	 * @return
	 */
	public Router getRouter() {
		return this.router;
	}

	/**
	 * Return or create the custom router which will be the base router for all custom endpoints which can be accessed using <code>/custom</code>.
	 * 
	 * @param name
	 *            Name of the custom sub router
	 * @return Found or created router
	 */
	public Router getCustomSubRouter(String name) {
		Router router = customRouters.get(name);
		if (router == null) {
			router = Router.router(vertx);
			log.info("Added custom subrouter {" + name + "}");
			customRouters.put(name, router);
		}
		router.mountSubRouter("/" + name, router);
		return router;
	}

}
