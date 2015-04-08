package com.gentics.cailun.core;

import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.RoutingContext;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
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

	private static final Object LANGUAGES_QUERY_PARAM_KEY = "lang";
	private static final Object DEPTH_PARAM_KEY = "depth";

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

	/**
	 * Extracts the lang parameter values from the query.
	 * 
	 * @param rc
	 * @return List of languages. List can be empty.
	 */
	protected List<String> getSelectedLanguageTags(RoutingContext rc) {
		String query = rc.request().query();
		Map<String, String> queryPairs;
		try {
			queryPairs = splitQuery(query);
		} catch (UnsupportedEncodingException e) {
			log.error("Could not decode query string.", e);
			return new ArrayList<>();
		}
		String value = queryPairs.get(LANGUAGES_QUERY_PARAM_KEY);
		if (value == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(Arrays.asList(value.split(",")));

	}

	protected int getDepth(RoutingContext rc) {
		String query = rc.request().query();
		Map<String, String> queryPairs;
		try {
			queryPairs = splitQuery(query);
		} catch (UnsupportedEncodingException e) {
			log.error("Could not decode query string.", e);
			return 0;
		}
		String value = queryPairs.get(DEPTH_PARAM_KEY);
		return NumberUtils.toInt(value, 0);
	}

}
