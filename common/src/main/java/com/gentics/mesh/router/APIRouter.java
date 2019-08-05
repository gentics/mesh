package com.gentics.mesh.router;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.VersionHandler;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;

public class APIRouter {

	private static final Logger log = LoggerFactory.getLogger(APIRouter.class);

	private final ProjectsRouter projectsRouter;
	private final PluginRouter pluginRouter;

	/**
	 * The api router is a core router which is being used to identify the api and rest api version.
	 */
	private final Router router;

	/**
	 * API routers are routers that are responsible for dealing with routes that are no project routes. E.g: :apibase:/admin, /api/v2
	 */
	private Map<String, Router> apiRouters = new HashMap<>();

	private final Vertx vertx;

	private RootRouter root;

	private final MeshOptions options;

	public APIRouter(Vertx vertx, RootRouter root, MeshOptions options) {
		this.vertx = vertx;
		this.root = root;
		this.options = options;
		this.router = Router.router(vertx);

		VersionHandler.generateVersionMountpoints()
			.forEach(mountPoint -> root.getRouter().mountSubRouter(mountPoint, router));

		initHandlers(root.getStorage());

		this.projectsRouter = new ProjectsRouter(vertx, this);
		this.pluginRouter = new PluginRouter(vertx, root.getStorage().getAuthChain(), root.getStorage().getDb().get(), getRouter());
	}

	private void initHandlers(RouterStorage storage) {
		if (options.getHttpServerOptions().isCorsEnabled()) {
			router.route().handler(storage.corsHandler);
		}

		router.route().handler(rh -> {
			// Connection upgrade requests never end and therefore the body
			// handler will never pass through to the subsequent route handlers.
			if ("websocket".equalsIgnoreCase(rh.request().getHeader("Upgrade"))) {
				rh.next();
			} else {
				storage.bodyHandler.handle(rh);
			}
		});

		router.route().handler(CookieHandler.create());

	}

	/**
	 * Returns the plugin router which can be used to create routers for plugins.
	 * 
	 * @return
	 */
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
			apiSubRouter = Router.router(vertx);
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
