package com.gentics.mesh.router;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.Mesh;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

public class CustomRouter {

	private static final Logger log = LoggerFactory.getLogger(CustomRouter.class);

	public static final String CUSTOM_MOUNTPOINT = "/custom";

	private final Router router;

	/**
	 * Custom routers. (E.g.: /demo)
	 */
	private Map<String, Router> customRouters = new HashMap<>();

	public CustomRouter(RootRouter root) {
		Router rootRouter = root.getRouter();
		router = Router.router(Mesh.vertx());
		rootRouter.mountSubRouter(CUSTOM_MOUNTPOINT, router);
	}

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
			router = Router.router(Mesh.vertx());
			log.info("Added custom subrouter {" + name + "}");
			customRouters.put(name, router);
		}
		router.mountSubRouter("/" + name, router);
		return router;
	}

}
