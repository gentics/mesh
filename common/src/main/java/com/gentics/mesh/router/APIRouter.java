package com.gentics.mesh.router;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.Mesh;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.CorsHandler;

public class APIRouter {

	private static final Logger log = LoggerFactory.getLogger(APIRouter.class);

	public static final String API_MOUNTPOINT = "/api/v1";

	private final ProjectsRouter projectsRouter;
	private final PluginRouter pluginRouter;

	/**
	 * The api router is a core router which is being used to identify the api and rest api version.
	 */
	private final Router router;

	/**
	 * API routers are routers that are responsible for dealing with routes that are no project routes. E.g: /api/v1/admin, /api/v1
	 */
	private Map<String, Router> apiRouters = new HashMap<>();

	private RootRouter root;

	public APIRouter(RootRouter root) {
		this.root = root;
		this.router = Router.router(Mesh.vertx());
		root.getRouter().mountSubRouter(API_MOUNTPOINT, router);

		initHandlers(root.getStorage().corsHandler, root.getStorage().bodyHandler);

		this.projectsRouter = new ProjectsRouter(this);
		this.pluginRouter = new PluginRouter(router);
	}

	private void initHandlers(CorsHandler corsHandler, BodyHandler bodyHandler) {
		if (Mesh.mesh().getOptions().getHttpServerOptions().isCorsEnabled()) {
			router.route().handler(corsHandler);
		}

		router.route().handler(rh -> {
			// Connection upgrade requests never end and therefore the body
			// handler will never
			// pass through to the subsequent route handlers.
			if ("websocket".equalsIgnoreCase(rh.request().getHeader("Upgrade"))) {
				rh.next();
			} else {
				bodyHandler.handle(rh);
			}
		});
		router.route().handler(CookieHandler.create());

	}

	public PluginRouter pluginRouter() {
		return pluginRouter;
	}

	/**
	 * Return the router to which all projects will be mounted.
	 * 
	 * @return
	 */
	public ProjectsRouter projectsRouter() {
		return projectsRouter;
	}

	public Router getRouter() {
		return router;
	}

	/**
	 * Get a core api subrouter. A new router will be created id no existing one could be found.
	 * 
	 * @param mountPoint
	 * @return existing or new router
	 */
	public Router createSubRouter(String mountPoint) {

		// TODO check for conflicting project routers
		Router apiSubRouter = apiRouters.get(mountPoint);
		if (apiSubRouter == null) {
			apiSubRouter = Router.router(Mesh.vertx());
			if (log.isDebugEnabled()) {
				log.debug("Creating API subrouter for {" + mountPoint + "}");
			}
			router.mountSubRouter("/" + mountPoint, apiSubRouter);
			apiRouters.put(mountPoint, apiSubRouter);
		}
		return apiSubRouter;

	}

	public Map<String, Router> getRouters() {
		return apiRouters;
	}

	public RootRouter getRoot() {
		return root;
	}

}
