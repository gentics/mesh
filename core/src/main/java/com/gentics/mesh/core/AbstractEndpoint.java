package com.gentics.mesh.core;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.example.GroupExamples;
import com.gentics.mesh.example.MicroschemaExamples;
import com.gentics.mesh.example.MiscExamples;
import com.gentics.mesh.example.NodeExamples;
import com.gentics.mesh.example.ProjectExamples;
import com.gentics.mesh.example.RoleExamples;
import com.gentics.mesh.example.SchemaExamples;
import com.gentics.mesh.example.TagExamples;
import com.gentics.mesh.example.TagFamilyExamples;
import com.gentics.mesh.example.UserExamples;
import com.gentics.mesh.example.VersioningExamples;
import com.gentics.mesh.rest.Endpoint;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

/**
 * An abstract class that should be used when creating verticles which expose a http server. The verticle will automatically start a http server and add the
 * http server handler to the core router storage handler.
 */
public abstract class AbstractEndpoint {

	private List<Endpoint> endpoints = new ArrayList<>();

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

	protected Router localRouter = null;
	protected String basePath;

	protected RouterStorage routerStorage;

	@Inject
	public MeshAuthHandler authHandler;

	protected AbstractEndpoint(String basePath, RouterStorage routerStorage) {
		this.basePath = basePath;
		this.routerStorage = routerStorage;
		this.localRouter = setupLocalRouter();
	}

	/**
	 * Add a route which will secure all endpoints.
	 */
	protected void secureAll() {
		getRouter().route("/*").handler(authHandler);
	}

	/**
	 * Register all endpoints to the local router.
	 * 
	 * @throws Exception
	 */
	public abstract void registerEndPoints() throws Exception;

	/**
	 * Description of the endpoints in a broader scope.
	 * 
	 * @return
	 */
	public abstract String getDescription();

	public Router setupLocalRouter() {
		return routerStorage.getAPISubRouter(basePath);
	}

	public Router getRouter() {
		return localRouter;
	}

	/**
	 * Wrapper for getRouter().route(path)
	 * 
	 * @param path
	 * @return
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

	protected Endpoint createEndpoint() {
		Endpoint endpoint = new Endpoint(getRouter());
		endpoints.add(endpoint);
		return endpoint;
	}

	public List<Endpoint> getEndpoints() {
		return endpoints;
	}

	public String getBasePath() {
		return basePath;
	}

}
