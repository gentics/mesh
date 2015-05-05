package com.gentics.cailun.core;

import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.RoutingContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.verticle.ContentVerticle;
import com.gentics.cailun.etc.RouterStorage;

/**
 * A cailun project rest verticle is a verticle that provides rest endpoints for all registered projects. The router for this verticle will automatically be
 * mounted for all registered projects. E.g: /api/v1/yourproject/verticle_basePath
 * 
 * @author johannes2
 *
 */
public abstract class AbstractProjectRestVerticle extends AbstractRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(ContentVerticle.class);

	@Autowired
	protected ProjectService projectService;

	protected AbstractProjectRestVerticle(String basePath) {
		super(basePath);
	}

	@Override
	public Router setupLocalRouter() {
		Router localRouter = routerStorage.getProjectSubRouter(basePath);
		return localRouter;
	}

	/**
	 * Extracts the project name from the routing context
	 * 
	 * @param rc
	 * @return extracted project name
	 */
	protected String getProjectName(RoutingContext rc) {
		return rc.get(RouterStorage.PROJECT_CONTEXT_KEY);
	}

}
