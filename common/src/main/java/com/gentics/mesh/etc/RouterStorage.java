package com.gentics.mesh.etc;

import static com.gentics.mesh.Events.EVENT_PROJECT_CREATED;
import static com.gentics.mesh.Events.EVENT_PROJECT_UPDATED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.URIUtils.encodeFragment;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import dagger.Lazy;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.CorsHandler;
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
@Singleton
public class RouterStorage {

	private static final Logger log = LoggerFactory.getLogger(RouterStorage.class);

	private static final String ROOT_ROUTER_KEY = "ROOT_ROUTER";
	private static final String API_ROUTER_KEY = "API_ROUTER";
	private static final String CUSTOM_ROUTER_KEY = "CUSTOM_ROUTER";

	public static final String DEFAULT_API_MOUNTPOINT = "/api/v1";
	public static final String DEFAULT_CUSTOM_MOUNTPOINT = "/custom";
	public static final String PROJECT_CONTEXT_KEY = "mesh-project";

	private static RouterStorage instance;

	private Lazy<BootstrapInitializer> boot;

	private Lazy<Database> db;

	private CorsHandler corsHandler;
	private Handler<RoutingContext> bodyHandler;

	@Inject
	public RouterStorage(CorsHandler corsHandler, Handler<RoutingContext> bodyHandler, Lazy<BootstrapInitializer> boot, Lazy<Database> db) {
		this.boot = boot;
		this.db = db;
		this.corsHandler = corsHandler;
		this.bodyHandler = bodyHandler;
		RouterStorage.instance = this;
	}

	public static RouterStorage getIntance() {
		return instance;
	}

	public void registerEventbusHandlers() {
		EventBus eb = Mesh.vertx().eventBus();
		eb.consumer(EVENT_PROJECT_CREATED, (Message<JsonObject> rh) -> {
			JsonObject json = rh.body();
			String name = json.getString("name");
			try {
				RouterStorage.getIntance().addProjectRouter(name);
				if (log.isInfoEnabled()) {
					log.info("Registered project {" + name + "}");
				}
			} catch (InvalidNameException e) {
				// TODO should we really fail here?
				throw error(BAD_REQUEST, "Error while adding project to router storage", e);
			}
		});

		eb.consumer(EVENT_PROJECT_UPDATED, (Message<JsonObject> rh) -> {
			RouterStorage routerStorage = RouterStorage.getIntance();
			Database database = db.get();

			try (Tx tx = database.tx()) {
				Set<String> projectNames = new HashSet<>();
				// Check whether there are any projects which do not have an active project router
				for (Project project : boot.get().projectRoot().findAllIt()) {
					if (!routerStorage.hasProjectRouter(project.getName())) {
						log.info("Mounting project {" + project.getName() + "}");
						routerStorage.addProjectRouter(project.getName());
					}
					projectNames.add(project.getName());
				}

				// Check whether there are any project routers which are no longer valid / in-sync with the projects.
				for (String projectName : routerStorage.getProjectRouters().keySet()) {
					if (!projectNames.contains(projectName)) {
						log.info("Removing invalid mount {" + projectName + "}");
						routerStorage.removeProjectRouter(projectName);
					}
				}
			} catch (InvalidNameException e) {
				log.error("Could not update project routers", e);
				rh.fail(400, "Invalid project name found");
			}

		});

	}

	/**
	 * Initialize the router storage. This will setup the basic route handlers for /api/v1 and cookie/cors handling.
	 */
	public void init() {
		initAPIRouter(corsHandler, bodyHandler);
	}

	/**
	 * Core routers are routers that are responsible for dealing with routes that are no project routes. E.g: /api/v1/admin, /api/v1
	 */
	private Map<String, Router> coreRouters = new ConcurrentHashMap<>();

	/**
	 * Custom routers. (E.g.: /demo)
	 */
	private Map<String, Router> customRouters = new ConcurrentHashMap<>();

	/**
	 * Project routers are routers that handle project rest api endpoints. E.g: /api/v1/dummy, /api/v1/yourprojectname
	 */
	private Map<String, Router> projectRouters = new ConcurrentHashMap<>();

	/**
	 * Project sub routers are routers that are mounted by project routers. E.g: /api/v1/dummy/nodes, /api/v1/yourprojectname/tagFamilies
	 */
	private Map<String, Router> projectSubRouters = new ConcurrentHashMap<>();

	/**
	 * The root {@link Router} is a core router that is used as a parent for all other routers. This method will create the root router if non is existing.
	 * 
	 * @return the root router
	 */
	public Router getRootRouter() {
		Router rootRouter = coreRouters.get(ROOT_ROUTER_KEY);
		if (rootRouter == null) {
			rootRouter = Router.router(Mesh.vertx());

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
	 * Initialise the Root API router and add common handlers to the router. The API router is used to attach subrouters for routes like
	 * /api/v1/[groups|users|roles]
	 */
	private void initAPIRouter(CorsHandler corsHandler, Handler<RoutingContext> bodyHandler) {
		Router router = getAPIRouter();
		if (Mesh.mesh().getOptions().getHttpServerOptions().isCorsEnabled()) {
			router.route().handler(corsHandler);
		}
		router.route().handler(bodyHandler);
		router.route().handler(CookieHandler.create());
	}

	/**
	 * Return or create the custom router which will be the base router for all custom verticles which can be accessed using <code>/custom</code>.
	 * 
	 * @return
	 */
	public Router getCustomRouter() {
		Router customRouter = coreRouters.get(CUSTOM_ROUTER_KEY);
		if (customRouter == null) {
			customRouter = Router.router(Mesh.vertx());

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
			apiRouter = Router.router(Mesh.vertx());
			coreRouters.put(API_ROUTER_KEY, apiRouter);
			getRootRouter().mountSubRouter(DEFAULT_API_MOUNTPOINT, apiRouter);
		}
		return apiRouter;

	}

	/**
	 * Return the map with core routers.
	 * 
	 * @return
	 */
	public Map<String, Router> getCoreRouters() {
		return coreRouters;
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
			apiSubRouter = Router.router(Mesh.vertx());
			if (log.isDebugEnabled()) {
				log.debug("Creating subrouter for {" + mountPoint + "}");
			}
			getAPIRouter().mountSubRouter("/" + mountPoint, apiSubRouter);
			coreRouters.put(mountPoint, apiSubRouter);
		}
		return apiSubRouter;

	}

	/**
	 * Remove the project router with the given name from the list of project router.
	 * 
	 * @param name
	 *            Name of the project router
	 * @return true if the project router could be removed. Otherwise false
	 */
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
	 * Check whether the project router for the given project name is already registered.
	 * 
	 * @param projectName
	 * @return
	 */
	public boolean hasProjectRouter(String projectName) {
		return projectRouters.containsKey(projectName);
	}

	/**
	 * Add a new project router with the given name to the api router. This method will return an existing router when one already has been setup.
	 * 
	 * @param name
	 *            Name of the project router
	 * @return Router for the given project name
	 * @throws InvalidNameException
	 */
	public Router addProjectRouter(String name) throws InvalidNameException {
		String encodedName = encodeFragment(name);
		assertProjectNameValid(name);
		Router projectRouter = projectRouters.get(name);
		// TODO synchronise access to projectRouters
		if (projectRouter == null) {
			projectRouter = Router.router(Mesh.vertx());
			projectRouters.put(name, projectRouter);
			log.info("Added project router {" + name + "}");

			projectRouter.route().handler(ctx -> {
				Project project = db.get().tx(() -> boot.get().projectRoot().findByName(name));
				if (log.isDebugEnabled()) {
					if (project == null) {
						log.debug("Project for name {" + name + "} could not be found.");
					}
				}
				ctx.data().put(PROJECT_CONTEXT_KEY, project);
				ctx.next();
			});

			getAPIRouter().mountSubRouter("/" + encodedName, projectRouter);
			mountSubRoutersForProjectRouter(projectRouter, encodedName);
		}
		return projectRouter;
	}

	/**
	 * Returns a map of all registered project routers.
	 * 
	 * @return
	 */
	public Map<String, Router> getProjectRouters() {
		return projectRouters;
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
			router = Router.router(Mesh.vertx());
			log.info("Added project subrouter {" + name + "}");
			projectSubRouters.put(name, router);
		}
		mountRouterInProjects(router, name);
		return router;
	}

	/**
	 * Return custom sub router.
	 * 
	 * @param name
	 *            Name of the custom sub router
	 * @return Found or created router
	 */
	public Router getCustomSubRouter(String name) {
		Router router = customRouters.get(name);
		if (router == null) {
			router = Router.router(Mesh.vertx());
			log.info("Added custom subrouter {" + name + "}");
			customRouters.put(name, router);
		}
		getCustomRouter().mountSubRouter("/" + name, router);
		return router;
	}

	/**
	 * Fail if the provided name is invalid or would cause a conflicts with an existing API router.
	 * 
	 * @param name
	 *            Project name to be checked
	 */
	public void assertProjectNameValid(String name) {
		String encodedName = encodeFragment(name);
		if (coreRouters.containsKey(name) || coreRouters.containsKey(encodedName)) {
			throw error(BAD_REQUEST, "project_error_name_already_reserved", name);
		}
	}

}
