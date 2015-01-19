package com.gentics.vertx.cailun.rest;

import io.vertx.core.Vertx;
import io.vertx.ext.apex.core.Router;

import java.util.HashMap;
import java.util.Map;

public class RouterStorage {

	private Vertx vertx;
	private static final String ROOT_ROUTER_KEY = "ROOT_ROUTER";
	private static final String API_ROUTER_KEY = "API_ROUTER";
	private static RouterStorage instance;

	// overlapping routers are currently not supported:
	// e.g: /page/page will not work
	private Map<String, Router> routers = new HashMap<>();

	private RouterStorage(Vertx vertx) {
		this.vertx = vertx;
	}

	public Router getRootRouter() {
		if (routers.keySet().contains(ROOT_ROUTER_KEY)) {
			return routers.get(ROOT_ROUTER_KEY);
		} else {
			Router rootRouter = Router.router(vertx);
			routers.put(ROOT_ROUTER_KEY, rootRouter);
			return rootRouter;
		}
	}

	public Router getAPIRouter() {
		if (routers.keySet().contains(API_ROUTER_KEY)) {
			return routers.get(API_ROUTER_KEY);
		} else {
			Router apiRouter = Router.router(vertx);
			routers.put(API_ROUTER_KEY, apiRouter);
			getRootRouter().mountSubRouter("/api/v1", apiRouter);
			return apiRouter;
		}

	}

	public Router getRouter(String mountPoint) {

		if (routers.keySet().contains(mountPoint)) {
			return routers.get(mountPoint);
		} else {
			Router localRouter = Router.router(vertx);
			getAPIRouter().mountSubRouter(mountPoint, localRouter);
			routers.put(mountPoint, localRouter);
			return localRouter;
		}
	}

	public static RouterStorage getInstance(Vertx vertx) {
		if (instance == null) {
			instance = new RouterStorage(vertx);
		}
		return instance;
	}

}
