package com.gentics.mesh.router.route;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.rest.EndpointRoute;
import com.gentics.mesh.rest.impl.EndpointImpl;
import com.gentics.mesh.router.RouterStorage;

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

	@Inject
	public MeshAuthHandler authHandler;

	protected RouterStorage routerStorage;

	/**
	 * Constructor to be invoked from implementation.
	 * 
	 * @param basePath
	 *            Basebase for the endpoint
	 * @param routerStorage
	 *            Router storage
	 */
	protected AbstractEndpoint(String basePath) {
		this.basePath = basePath;
	}

	public void init(RouterStorage rs) {
		this.routerStorage = rs;
		this.localRouter = rs.getAPISubRouter(basePath);
	}

	/**
	 * Add a route which will secure all endpoints.
	 */
	protected void secureAll() {
		getRouter().route("/*").handler(authHandler);
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
