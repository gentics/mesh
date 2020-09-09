package com.gentics.mesh.router;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * @see ProjectRouter
 */
public class ProjectRouterImpl implements ProjectRouter {

	private static final Logger log = LoggerFactory.getLogger(ProjectRouterImpl.class);

	private final Vertx vertx;

	private final PluginRouter pluginRouter;

	private final Router router;

	/**
	 * Project routers are routers that are mounted by project routers. E.g: :apibase:/dummy/nodes, :apibase:/yourprojectname/tagFamilies
	 */
	private Map<String, Router> projectRouters = new HashMap<>();

	public ProjectRouterImpl(Vertx vertx, RouterStorage storage) {
		this.vertx = vertx;
		this.router = Router.router(vertx);
		this.pluginRouter = new PluginRouterImpl(vertx, storage.getAuthChain(), (Database) storage.getDb(), router);
	}

	@Override
	public Router getOrCreate(String name) {
		Router projectRouter = projectRouters.get(name);
		if (projectRouter == null) {
			projectRouter = Router.router(vertx);
			projectRouters.put(name, projectRouter);
			log.info("Added project subrouter {" + name + "}");
			router.mountSubRouter("/" + name, projectRouter);
		}
		return projectRouter;
	}

	@Override
	public Router getRouter() {
		return router;
	}

	@Override
	public PluginRouter pluginRouter() {
		return pluginRouter;
	}

	@Override
	public Map<String, Router> getRouters() {
		return projectRouters;
	}

}
