package com.gentics.mesh.router.route;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.rest.InternalEndpoint;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.rest.impl.InternalEndpointRouteImpl;
import com.gentics.mesh.router.RouterStorage;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

/**
 * An abstract class that should be used when creating new endpoints.
 */
public abstract class AbstractInternalEndpoint implements InternalEndpoint {

	protected List<InternalEndpointRoute> endpointRoutes = new ArrayList<>();

	protected Router localRouter = null;

	protected String basePath;

	protected MeshAuthChain chain;

	protected RouterStorage routerStorage;

	protected AbstractInternalEndpoint(String basePath, MeshAuthChain chain) {
		this.basePath = basePath;
		this.chain = chain;
	}

	public void init(Vertx vertx, RouterStorage rs) {
		this.routerStorage = rs;
		this.localRouter = rs.root().apiRouter().createSubRouter(basePath);
	}

	/**
	 * Add a route which will secure all endpoints.
	 */
	protected void secureAll() {
		if (chain != null) {
			chain.secure(getRouter().route());
		}
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
	public InternalEndpointRoute createRoute() {
		InternalEndpointRoute endpoint = new InternalEndpointRouteImpl(getRouter());
		endpointRoutes.add(endpoint);
		return endpoint;
	}

	/**
	 * Return a list of all endpoints that have been registered within this endpoint.
	 * 
	 * @return List of created endpoint
	 */
	public List<InternalEndpointRoute> getEndpoints() {
		return endpointRoutes;
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
