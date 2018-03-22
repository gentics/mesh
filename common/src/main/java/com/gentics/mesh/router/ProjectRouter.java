package com.gentics.mesh.router;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.Mesh;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * This class manages the project routers (e.g. routers for endpoints like /api/v1/:projectName/nodes)
 */
public class ProjectRouter {

	private static final Logger log = LoggerFactory.getLogger(ProjectRouter.class);

	private final PluginRouter pluginRouter;

	private final Router router;

	/**
	 * Project routers are routers that are mounted by project routers. E.g: /api/v1/dummy/nodes, /api/v1/yourprojectname/tagFamilies
	 */
	private Map<String, Router> projectRouters = new HashMap<>();

	public ProjectRouter(RouterStorage storage) {
		this.router = Router.router(Mesh.vertx());
		this.pluginRouter = new PluginRouter(router);
	}

	/**
	 * Return the registered project subrouter.
	 * 
	 * @return the router or null if no router was found
	 */
	public Router getOrCreate(String name) {
		Router projectRouter = projectRouters.get(name);
		if (projectRouter == null) {
			projectRouter = Router.router(Mesh.vertx());
			projectRouters.put(name, projectRouter);
			log.info("Added project subrouter {" + name + "}");
			router.mountSubRouter("/" + name, projectRouter);
		}
		return projectRouter;
	}

	public Router getRouter() {
		return router;
	}

	public PluginRouter pluginRouter() {
		return pluginRouter;
	}

	public Map<String, Router> getRouters() {
		return projectRouters;
	}

}
