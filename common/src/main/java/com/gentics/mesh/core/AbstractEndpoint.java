package com.gentics.mesh.core;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.example.AdminExamples;
import com.gentics.mesh.example.AssetExamples;
import com.gentics.mesh.example.GraphQLExamples;
import com.gentics.mesh.example.GroupExamples;
import com.gentics.mesh.example.JobExamples;
import com.gentics.mesh.example.MicroschemaExamples;
import com.gentics.mesh.example.MiscExamples;
import com.gentics.mesh.example.NodeExamples;
import com.gentics.mesh.example.ProjectExamples;
import com.gentics.mesh.example.ReleaseExamples;
import com.gentics.mesh.example.RoleExamples;
import com.gentics.mesh.example.SchemaExamples;
import com.gentics.mesh.example.TagExamples;
import com.gentics.mesh.example.TagFamilyExamples;
import com.gentics.mesh.example.UserExamples;
import com.gentics.mesh.example.VersioningExamples;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.rest.impl.EndpointImpl;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

/**
 * An abstract class that should be used when creating new endpoints.
 */
public abstract class AbstractEndpoint {

	protected List<Endpoint> endpoints = new ArrayList<>();

	protected NodeExamples nodeExamples = new NodeExamples();
	protected TagExamples tagExamples = new TagExamples();
	protected TagFamilyExamples tagFamilyExamples = new TagFamilyExamples();
	protected GroupExamples groupExamples = new GroupExamples();
	protected RoleExamples roleExamples = new RoleExamples();
	protected MiscExamples miscExamples = new MiscExamples();
	protected VersioningExamples versioningExamples = new VersioningExamples();
	protected SchemaExamples schemaExamples = new SchemaExamples();
	protected ProjectExamples projectExamples = new ProjectExamples();
	protected UserExamples userExamples = new UserExamples();
	protected MicroschemaExamples microschemaExamples = new MicroschemaExamples();
	protected GraphQLExamples graphqlExamples = new GraphQLExamples();
	protected AdminExamples adminExamples = new AdminExamples();
	protected JobExamples jobExamples = new JobExamples();
	protected ReleaseExamples releaseExamples = new ReleaseExamples();
	protected AssetExamples assetExamples = new AssetExamples();

	protected Router localRouter = null;
	protected String basePath;

	protected RouterStorage routerStorage;

	@Inject
	public MeshAuthHandler authHandler;

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

	/**
	 * Create a new endpoint. Internally a new route will be wrapped.
	 * 
	 * @return Created endpoint
	 */
	protected Endpoint createEndpoint() {
		Endpoint endpoint = new EndpointImpl(getRouter());
		endpoints.add(endpoint);
		return endpoint;
	}

	/**
	 * Return a list of all endpoints that have been registered within this endpoint.
	 * 
	 * @return List of created endpoint
	 */
	public List<Endpoint> getEndpoints() {
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
