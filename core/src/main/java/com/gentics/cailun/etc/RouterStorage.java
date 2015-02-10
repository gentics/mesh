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
import com.gentics.cailun.etc.config.CaiLunConfigurationException;

/**
 * Central storage for all apex request routers.
 * 
 * @author johannes2
 *
 */
@NoArgsConstructor
public class RouterStorage {

	private Vertx vertx;
	private static final String ROOT_ROUTER_KEY = "ROOT_ROUTER";
	private static final String API_ROUTER_KEY = "API_ROUTER";
	private static final String DEFAULT_API_MOUNTPOINT = "/api/v1";

	/**
	 * Core routers are routers that are responsible for dealing with routes that are no project routes. E.g: /api/v1/admin, /api/v1
	 */
	private Map<String, Router> coreRouters = new HashMap<>();

	/**
	 * Project routers are routers that handle project rest api endpoints. E.g: /api/v1/alohaeditor, /api/v1/yourprojectname
	 */
	private Map<String, Router> projectRouters = new HashMap<>();

	/**
	 * Project sub routers are routers that are mounted by project routers. E.g: /api/v1/alohaeditor/contents, /api/v1/yourprojectname/tags
	 */
	private Map<String, Router> projectSubRouters = new HashMap<>();

	public RouterStorage(Vertx vertx, CaiLunAuthServiceImpl caiLunAuthServiceImpl) {
		this.vertx = vertx;
		initAPIRouter(caiLunAuthServiceImpl);
	}

	/**
	 * The root {@link Router} is a core router that is used as a parent for all other routers. This method will create the root router if non is existing.
	 * 
	 * @return the root router
	 */
	public Router getRootRouter() {
		Router rootRouter = coreRouters.get(ROOT_ROUTER_KEY);
		if (rootRouter == null) {
			rootRouter = Router.router(vertx);
			// TODO use template engine to render a fancy error page and log the error.
			// TODO somehow this failurehandler prevents authentication?
			// rootRouter.route().failureHandler(fctx -> {
			// if (fctx.failure() != null) {
			// String exception = Throwables.getStackTraceAsString(fctx.failure());
			// fctx.response().end("Error: " + exception);
			// } else {
			// fctx.response().end("Error: unknown error");
			// }
			// });
			coreRouters.put(ROOT_ROUTER_KEY, rootRouter);
		}
		return rootRouter;
	}

	// // TODO I think this functionality should be moved to a different place
	private void initAPIRouter(CaiLunAuthServiceImpl caiLunAuthServiceImpl) {
		Router router = getAPIRouter();
		router.route().handler(BodyHandler.bodyHandler());
		router.route().handler(CookieHandler.cookieHandler());
		SessionStore store = LocalSessionStore.localSessionStore(vertx);
		router.route().handler(new SessionHandlerImpl("cailun.session", 30 * 60 * 1000, false, store));
		AuthHandler authHandler = BasicAuthHandler.basicAuthHandler(caiLunAuthServiceImpl, BasicAuthHandler.DEFAULT_REALM);
		router.route().handler(authHandler);
	}

	/**
	 * The api router is a core router which is being used to identify the api and rest api version. This method will create a api router if non is existing.
	 * 
	 * @return api router
	 */
	public Router getAPIRouter() {
		if (coreRouters.keySet().contains(API_ROUTER_KEY)) {
			return coreRouters.get(API_ROUTER_KEY);
		} else {
			Router apiRouter = Router.router(vertx);
			coreRouters.put(API_ROUTER_KEY, apiRouter);
			getRootRouter().mountSubRouter(DEFAULT_API_MOUNTPOINT, apiRouter);
			return apiRouter;
		}

	}

	/**
	 * Get a core api subrouter. A new router will be created id no existing one could be found.
	 * 
	 * @param mountPoint
	 * @return existing or new router
	 */
	public Router getAPISubRouter(String mountPoint) {
		Router apiSubRouter = coreRouters.get(mountPoint);
		if (apiSubRouter == null) {
			apiSubRouter = Router.router(vertx);
			getAPIRouter().mountSubRouter("/" + mountPoint, apiSubRouter);
			coreRouters.put(mountPoint, apiSubRouter);
		}
		return apiSubRouter;

	}

	public boolean removeProjectRouter(String name) {
		Router projectRouter = projectRouters.get(name);
		if (projectRouter == null) {
			return false;
		} else {
			// TODO umount router from api router?
			projectRouter.clear();
			projectRouters.remove(name);
			// TODO remove from all routers?
			return true;
		}

	}

	/**
	 * Add a new project router with the given name to the api router. This method will return an existing router when one already has been setup.
	 * 
	 * @param name
	 * @return
	 */
	public Router addProjectRouter(String name) {
		Router projectRouter;
		if (projectRouters.keySet().contains(name)) {
			projectRouter = projectRouters.get(name);
		} else {
			projectRouter = Router.router(vertx);
			projectRouters.put(name, projectRouter);
			getAPIRouter().mountSubRouter("/" + name, projectRouter);
			mountSubRoutersForProjectRouter(projectRouter);
		}
		return projectRouter;
	}

	/**
	 * Mount all registered project subrouters on the project router.
	 * 
	 * @param projectRouter
	 */
	private void mountSubRoutersForProjectRouter(Router projectRouter) {
		for (String mountPoint : projectSubRouters.keySet()) {
			Router projectSubRouter = projectRouters.get(mountPoint);
			projectRouter.mountSubRouter("/" + mountPoint, projectSubRouter);
		}
	}

	/**
	 * Mounts the given router in all registered project routers
	 * 
	 * @param localRouter
	 * @param mountPoint
	 */
	public void mountRouterInProjects(Router localRouter, String mountPoint) {
		for (Router projectRouter : projectRouters.values()) {
			projectRouter.mountSubRouter(mountPoint, localRouter);
		}
	}

	/**
	 * Return the registered project subrouter.
	 * 
	 * @return the router or null if no router was found
	 */
	public Router getProjectSubRouter(String name) {
		Router router = projectSubRouters.get(name);
		if (router == null) {
			router = Router.router(vertx);
			projectSubRouters.put(name, router);
		}
		mountRouterInProjects(router, name);
		return router;
	}

}
