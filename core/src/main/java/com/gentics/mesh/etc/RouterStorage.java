package com.gentics.mesh.etc;

import static com.gentics.mesh.util.URIUtils.encodeFragment;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.naming.InvalidNameException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.AuthenticationOptions.AuthenticationMethod;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.LoggerHandler;

/**
 * Central storage for all vertx web request routers.
 * 
 * Structure:
 * 
 * <pre>
 * {@code
 * ROOT_ROUTER(:coreRouter) -> customRouters
 *                          -> apiRouters -> apiSubRouter (eg: /users.., /roles..)
 *                          -> projectRouters (eg: /Dummy/nodes)
 * }
 * </pre>
 * 
 * Project routers are automatically bound to all projects. This way only a single node verticle is needed to handle all project requests.
 * 
 */
public class RouterStorage {

	private static final Logger log = LoggerFactory.getLogger(RouterStorage.class);

	private Vertx vertx;
	private static final String ROOT_ROUTER_KEY = "ROOT_ROUTER";
	private static final String API_ROUTER_KEY = "API_ROUTER";
	private static final String CUSTOM_ROUTER_KEY = "CUSTOM_ROUTER";

	public static final String DEFAULT_API_MOUNTPOINT = "/api/v1";
	public static final String DEFAULT_CUSTOM_MOUNTPOINT = "/custom";
	public static final String PROJECT_CONTEXT_KEY = "mesh-project";

	private static RouterStorage instance;

	private MeshSpringConfiguration springConfiguration;

	@Inject
	public RouterStorage(MeshSpringConfiguration springConfiguration) {
		this.springConfiguration = springConfiguration;
		this.vertx = Mesh.vertx();
		RouterStorage.instance = this;
	}

	public static RouterStorage getIntance() {
		return instance;
	}

	/**
	 * Core routers are routers that are responsible for dealing with routes that are no project routes. E.g: /api/v1/admin, /api/v1
	 */
	private Map<String, Router> coreRouters = new HashMap<>();

	private Map<String, Router> customRouters = new HashMap<>();
	/**
	 * Project routers are routers that handle project rest api endpoints. E.g: /api/v1/dummy, /api/v1/yourprojectname
	 */
	private Map<String, Router> projectRouters = new HashMap<>();

	/**
	 * Project sub routers are routers that are mounted by project routers. E.g: /api/v1/dummy/nodes, /api/v1/yourprojectname/tagFamilies
	 */
	private Map<String, Router> projectSubRouters = new HashMap<>();

	/**
	 * The root {@link Router} is a core router that is used as a parent for all other routers. This method will create the root router if non is existing.
	 * 
	 * @return the root router
	 */
	public Router getRootRouter() {
		Router rootRouter = coreRouters.get(ROOT_ROUTER_KEY);
		if (rootRouter == null) {
			rootRouter = Router.router(vertx);

			// Root handlers
			rootRouter.route().handler(LoggerHandler.create());
			// TODO add a dedicated error for api router that informs about APPLICATION_JSON requirements. This may not be true for other routes (eg. custom
			// routes)
			rootRouter.route().last().handler(DefaultNotFoundHandler.create());
			rootRouter.route().failureHandler(FailureHandler.create());
			coreRouters.put(ROOT_ROUTER_KEY, rootRouter);
		}
		return rootRouter;
	}

	/**
	 * Initialize the Root API router and add common handlers to the router. The API router is used to attach subrouters for routes like
	 * /api/v1/[groups|users|roles]
	 */
	private void initAPIRouter() {
		Router router = getAPIRouter();
		if (Mesh.mesh().getOptions().getHttpServerOptions().isCorsEnabled()) {
			router.route().handler(springConfiguration.corsHandler());
		}
		// TODO It would be good to have two body handler. One for fileuploads and one for post data handling
		router.route().handler(springConfiguration.bodyHandler());

		router.route().handler(CookieHandler.create());
		if (Mesh.mesh().getOptions().getAuthenticationOptions().getAuthenticationMethod() == AuthenticationMethod.BASIC_AUTH) {
			router.route().handler(springConfiguration.sessionHandler());
			router.route().handler(springConfiguration.userSessionHandler(null, null));
		}
	}

	/**
	 * Return or create the custom router which will be the base router for all custom verticles which can be accessed using <code>/custom</code>.
	 * 
	 * @return
	 */
	public Router getCustomRouter() {
		Router customRouter = coreRouters.get(CUSTOM_ROUTER_KEY);
		if (customRouter == null) {
			customRouter = Router.router(vertx);

			coreRouters.put(CUSTOM_ROUTER_KEY, customRouter);
			getRootRouter().mountSubRouter(DEFAULT_CUSTOM_MOUNTPOINT, customRouter);
		}
		return customRouter;

	}

	/**
	 * The api router is a core router which is being used to identify the api and rest api version. This method will create a api router if non is existing.
	 * 
	 * @return api router
	 */
	public Router getAPIRouter() {
		Router apiRouter = coreRouters.get(API_ROUTER_KEY);
		if (apiRouter == null) {
			apiRouter = Router.router(vertx);
			coreRouters.put(API_ROUTER_KEY, apiRouter);
			getRootRouter().mountSubRouter(DEFAULT_API_MOUNTPOINT, apiRouter);
		}
		return apiRouter;

	}

	/**
	 * Get a core api subrouter. A new router will be created id no existing one could be found.
	 * 
	 * @param mountPoint
	 * @return existing or new router
	 */
	public Router getAPISubRouter(String mountPoint) {

		// TODO check for conflicting project routers
		Router apiSubRouter = coreRouters.get(mountPoint);
		if (apiSubRouter == null) {
			apiSubRouter = Router.router(vertx);
			getAPIRouter().mountSubRouter("/" + mountPoint, apiSubRouter);
			coreRouters.put(mountPoint, apiSubRouter);
		}
		return apiSubRouter;

	}

	public boolean removeProjectRouter(String name) {
		Router projectRouter = projectRouters.get(name);
		if (projectRouter != null) {
			// TODO umount router from api router?
			projectRouter.clear();
			projectRouters.remove(name);
			// TODO remove from all routers?
			return true;
		}
		return false;

	}

	/**
	 * Add a new project router with the given name to the api router. This method will return an existing router when one already has been setup.
	 * 
	 * @param name
	 * @return Router for the given project name
	 * @throws InvalidNameException
	 */
	public Router addProjectRouter(String name) throws InvalidNameException {
		String encodedName = encodeFragment(name);
		if (coreRouters.containsKey(encodedName)) {
			throw new InvalidNameException("The project name {" + encodedName
					+ "} is conflicting with a core router. Best guess is that an core verticle is already occupying the name. Please choose a different name or remove the conflicting core verticle.");
		}
		Router projectRouter = projectRouters.get(encodedName);
		// TODO synchronize access to projectRouters
		if (projectRouter == null) {
			projectRouter = Router.router(vertx);
			projectRouters.put(name, projectRouter);
			log.info("Added project router {" + name + "}");

			projectRouter.route().handler(ctx -> {
				ctx.data().put(PROJECT_CONTEXT_KEY, name);
				ctx.next();
			});

			getAPIRouter().mountSubRouter("/" + encodedName, projectRouter);
			mountSubRoutersForProjectRouter(projectRouter, encodedName);
		}
		return projectRouter;
	}

	/**
	 * Mount all registered project subrouters on the project router.
	 * 
	 * @param projectRouter
	 * @param projectRouterName
	 *            Name of the project router
	 */
	private void mountSubRoutersForProjectRouter(Router projectRouter, String projectRouterName) {
		for (String mountPoint : projectSubRouters.keySet()) {
			log.info("Mounting subrouter {" + mountPoint + "} onto given project router. {" + projectRouterName + "}");
			Router projectSubRouter = projectSubRouters.get(mountPoint);
			projectRouter.mountSubRouter("/" + mountPoint, projectSubRouter);
		}
	}

	/**
	 * Mounts the given router in all registered project routers
	 * 
	 * @param localRouter
	 * @param mountPoint
	 */
	public void mountRouterInProjects(Router localRouter, String mountPoint) {
		for (Entry<String, Router> projectRouterEntry : projectRouters.entrySet()) {
			log.info("Mounting router onto project router {" + projectRouterEntry.getKey() + "} with mountpoint {" + mountPoint + "}");
			projectRouterEntry.getValue().mountSubRouter("/" + mountPoint, localRouter);
		}
	}

	/**
	 * Return the registered project subrouter.
	 * 
	 * @return the router or null if no router was found
	 */
	public Router getProjectSubRouter(String name) {
		Router router = projectSubRouters.get(name);
		if (router == null) {
			router = Router.router(vertx);
			log.info("Added project subrouter {" + name + "}");
			projectSubRouters.put(name, router);
		}
		mountRouterInProjects(router, name);
		return router;
	}

	public Router getCustomSubRouter(String name) {
		Router router = customRouters.get(name);
		if (router == null) {
			router = Router.router(vertx);
			log.info("Added custom subrouter {" + name + "}");
			customRouters.put(name, router);
		}
		getCustomRouter().mountSubRouter("/" + name, router);
		return router;
	}

}
