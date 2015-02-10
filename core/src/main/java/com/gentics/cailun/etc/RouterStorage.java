package com.gentics.cailun.etc;

import io.vertx.core.Vertx;
import io.vertx.ext.apex.addons.AuthHandler;
import io.vertx.ext.apex.addons.BasicAuthHandler;
import io.vertx.ext.apex.addons.LocalSessionStore;
import io.vertx.ext.apex.addons.impl.SessionHandlerImpl;
import io.vertx.ext.apex.core.BodyHandler;
import io.vertx.ext.apex.core.CookieHandler;
import io.vertx.ext.apex.core.Router;
import io.vertx.ext.apex.core.SessionStore;

import java.util.HashMap;
import java.util.Map;

import lombok.NoArgsConstructor;

import com.gentics.cailun.auth.CaiLunAuthServiceImpl;

@NoArgsConstructor
public class RouterStorage {

	private Vertx vertx;
	private static final String ROOT_ROUTER_KEY = "ROOT_ROUTER";
	private static final String API_ROUTER_KEY = "API_ROUTER";
	private static final String DEFAULT_API_MOUNTPOINT = "/api/v1";

	// overlapping routers are currently not supported:
	// e.g: /page/page will not work
	private Map<String, Router> routers = new HashMap<>();

	public RouterStorage(Vertx vertx, CaiLunAuthServiceImpl caiLunAuthServiceImpl) {
		this.vertx = vertx;
		initAPIRouter(caiLunAuthServiceImpl);
	}

	public Router getRootRouter() {
		if (routers.keySet().contains(ROOT_ROUTER_KEY)) {
			return routers.get(ROOT_ROUTER_KEY);
		} else {
			Router rootRouter = Router.router(vertx);
			// TODO use template engine to render a fancy error page and log the error.
//			rootRouter.route().failureHandler(fctx -> {
//				if (fctx.failure() != null) {
//					String exception = Throwables.getStackTraceAsString(fctx.failure());
//					fctx.response().end("Error: " + exception);
//				} else {
//					fctx.response().end("Error: unknown error");
//				}
//			});
			routers.put(ROOT_ROUTER_KEY, rootRouter);
			return rootRouter;
		}
	}

	// TODO I think this functionality should be moved to a different place
	private void initAPIRouter(CaiLunAuthServiceImpl caiLunAuthServiceImpl) {
		Router router = getAPIRouter();
		router.route().handler(BodyHandler.bodyHandler());
		router.route().handler(CookieHandler.cookieHandler());
		SessionStore store = LocalSessionStore.localSessionStore(vertx);
		router.route().handler(new SessionHandlerImpl("cailun.session", 30 * 60 * 1000, false, store));
		AuthHandler authHandler = BasicAuthHandler.basicAuthHandler(caiLunAuthServiceImpl, BasicAuthHandler.DEFAULT_REALM);
		router.route().handler(authHandler);
	}

	public Router getAPIRouter() {
		if (routers.keySet().contains(API_ROUTER_KEY)) {
			return routers.get(API_ROUTER_KEY);
		} else {
			Router apiRouter = Router.router(vertx);
			routers.put(API_ROUTER_KEY, apiRouter);
			getRootRouter().mountSubRouter(DEFAULT_API_MOUNTPOINT, apiRouter);
			return apiRouter;
		}

	}

	/**
	 * Get a root subrouter. A new router will be created id no existing one could be found.
	 * 
	 * @param mountPoint
	 * @return existing or new router
	 */
	public Router getRouter(String mountPoint) {
		if (routers.keySet().contains(mountPoint)) {
			return routers.get(mountPoint);
		} else {
			Router localRouter = Router.router(vertx);
			getRootRouter().mountSubRouter(mountPoint, localRouter);
			routers.put(mountPoint, localRouter);
			return localRouter;
		}

	}

	/**
	 * Get a local api router. A new router will be created if no existing one could be found.
	 * 
	 * @param mountPoint
	 * @return existing or new router
	 */
	public Router getLocalAPIRouter(String mountPoint) {
		if (routers.keySet().contains(DEFAULT_API_MOUNTPOINT + mountPoint)) {
			return routers.get(DEFAULT_API_MOUNTPOINT + mountPoint);
		} else {
			Router localRouter = Router.router(vertx);
			getAPIRouter().mountSubRouter(mountPoint, localRouter);
			routers.put(DEFAULT_API_MOUNTPOINT + mountPoint, localRouter);
			return localRouter;
		}
	}

}
