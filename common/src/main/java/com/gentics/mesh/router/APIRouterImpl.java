package com.gentics.mesh.router;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.VersionHandlerImpl;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;

/**
 * @see APIRouter
 */
public class APIRouterImpl implements APIRouter {

	private static final Logger log = LoggerFactory.getLogger(APIRouterImpl.class);

	private final ProjectsRouterImpl projectsRouter;
	private final PluginRouterImpl pluginRouter;

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

	public APIRouterImpl(Vertx vertx, RootRouter root, MeshOptions options) {
		this.vertx = vertx;
		this.root = root;
		this.options = options;
		this.router = Router.router(vertx);

		VersionHandlerImpl.generateVersionMountpoints()
			.forEach(mountPoint -> root.getRouter().mountSubRouter(mountPoint, router));

		initHandlers(root.getStorage());

		this.projectsRouter = new ProjectsRouterImpl(vertx, this);
		this.pluginRouter = new PluginRouterImpl(vertx, root.getStorage().getAuthChain(), (Database)root.getStorage().getDb(), getRouter());
	}

	private void initHandlers(RouterStorage storage) {
		// Add the coordinator delegator handler
		ClusterOptions clusterOptions = options.getClusterOptions();

		// add topology readonly handler
		if (clusterOptions.isEnabled() && clusterOptions.isTopologyChangeReadOnly()) {
			router.route().handler(root.getStorage().getTopologyChangeReadonlyHandler());
		}

		if (clusterOptions.isEnabled() && clusterOptions.getCoordinatorMode() != CoordinatorMode.DISABLED) {
			router.route().handler(root.getStorage().getDelegator());
		}

		if (options.getHttpServerOptions().isCorsEnabled()) {
			router.route().handler(storage.getCorsHandler());
		}

		router.route().handler(rh -> {
			// Connection upgrade requests never end and therefore the body
			// handler will never pass through to the subsequent route handlers.
			if ("websocket".equalsIgnoreCase(rh.request().getHeader("Upgrade"))) {
				rh.next();
			} else {
				storage.getBodyHandler().handle(rh);
			}
		});

		router.route().handler(CookieHandler.create());

	}

	@Override
	public PluginRouter pluginRouter() {
		return pluginRouter;
	}

	@Override
	public ProjectsRouter projectsRouter() {
		return projectsRouter;
	}

	@Override
	public Router getRouter() {
		return router;
	}

	@Override
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
