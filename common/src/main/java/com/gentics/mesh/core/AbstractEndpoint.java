package com.gentics.mesh.core;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.rest.EndpointRoute;
import com.gentics.mesh.rest.impl.EndpointImpl;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;

/**
 * An abstract class that should be used when creating new endpoints.
 */
public abstract class AbstractEndpoint implements Endpoint {

	protected List<EndpointRoute> endpoints = new ArrayList<>();

	protected Router localRouter = null;
	protected String basePath;

	protected RouterStorage routerStorage;

	@Inject
	public MeshAuthHandler authHandler;

	@Inject
	public BodyHandlerImpl bodyHandler;

	/**
	 * Constructor to be invoked from implementation.
	 * 
	 * @param basePath
	 *            Basebase for the endpoint
	 * @param routerStorage
	 *            Router storage
	 */
	protected AbstractEndpoint(String basePath, RouterStorage routerStorage) {
		this.basePath = basePath;
		this.routerStorage = routerStorage;
		if (routerStorage != null) {
			this.localRouter = setupLocalRouter();
		}
	}

	/**
	 * Add a route which will secure all endpoints.
	 */
	protected void secureAll() {
		getRouter().route("/*").handler(authHandler);
	}

	protected void withBodyHandler() {
		getRouter().route("/*").handler(bodyHandler);
	}

	/**
	 * Register all endpoints to the local router.
	 */
	public abstract void registerEndPoints();

	/**
	 * Description of the endpoints in a broader scope.
	 * 
	 * @return Description of the endpoint
	 */
	public abstract String getDescription();

	/**
	 * Setup the router for this endpoint using the endpoint basepath.
	 * 
	 * @return Router
	 */
	public Router setupLocalRouter() {
		return routerStorage.getAPISubRouter(basePath);
	}

	/**
	 * Return the created local router.
	 * 
	 * @return Router
	 */
	public Router getRouter() {
		return localRouter;
	}

	/**
	 * Wrapper for getRouter().route(path)
	 * 
	 * @param path
	 *            Path of the route
	 * @return Route
	 */
	protected Route route(String path) {
		Route route = getRouter().route(path);
		return route;
	}

	/**
	 * Wrapper for getRouter().route()
	 */
	protected Route route() {
		Route route = getRouter().route();
		return route;
	}

	@Override
	public EndpointRoute createEndpoint() {
		EndpointRoute endpoint = new EndpointImpl(getRouter());
		endpoints.add(endpoint);
		return endpoint;
	}

	/**
	 * Return a list of all endpoints that have been registered within this endpoint.
	 * 
	 * @return List of created endpoint
	 */
	public List<EndpointRoute> getEndpoints() {
		return endpoints;
	}

	/**
	 * Return the endpoint basepath.
	 * 
	 * @return Basepath
	 */
	public String getBasePath() {
		return basePath;
	}

}
