package com.gentics.mesh.etc;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.handler.CookieHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.naming.InvalidNameException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.http.LocaleContextDataHandler;
import com.gentics.mesh.core.rest.common.response.GenericMessageResponse;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.util.JsonUtils;

/**
 * Central storage for all apex request routers.
 * 
 * @author johannes2
 *
 */
@Component
@Scope(value = "singleton")
public class RouterStorage {

	private static final Logger log = LoggerFactory.getLogger(RouterStorage.class);

	private Vertx vertx;
	private static final String ROOT_ROUTER_KEY = "ROOT_ROUTER";
	private static final String API_ROUTER_KEY = "API_ROUTER";

	public static final String DEFAULT_API_MOUNTPOINT = "/api/v1";
	public static final String PROJECT_CONTEXT_KEY = "mesh-project";

	@Autowired
	private LocaleContextDataHandler dataHandler;

	@Autowired
	private MeshSpringConfiguration springConfiguration;

	@PostConstruct
	public void init() {
		this.vertx = springConfiguration.vertx();
		initAPIRouter();
	}

	/**
	 * Core routers are routers that are responsible for dealing with routes that are no project routes. E.g: /api/v1/admin, /api/v1
	 */
	private Map<String, Router> coreRouters = new HashMap<>();

	/**
	 * Project routers are routers that handle project rest api endpoints. E.g: /api/v1/alohaeditor, /api/v1/yourprojectname
	 */
	private Map<String, Router> projectRouters = new HashMap<>();

	/**
	 * Project sub routers are routers that are mounted by project routers. E.g: /api/v1/alohaeditor/contents, /api/v1/yourprojectname/tags
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

			// TODO Still valid: somehow this failurehandler prevents authentication?
			rootRouter.route().failureHandler(failureRoutingContext -> {
				if (failureRoutingContext.statusCode() == 401) {
					// Assume that it has been handled by the BasicAuthHandlerImpl
					log.debug("Got failure with 401 code.");
					failureRoutingContext.next();
				} else {
					Throwable failure = failureRoutingContext.failure();
					if (failure != null) {
						log.error("Error for request in path: " + failureRoutingContext.normalisedPath(), failure);
						int code = getResponseStatusCode(failure);
						failureRoutingContext.response().setStatusCode(code);
						failureRoutingContext.response().end(JsonUtils.toJson(new GenericMessageResponse(failure.getMessage())));
					} else {
						log.error("Error for request in path: " + failureRoutingContext.normalisedPath());
						failureRoutingContext.response().setStatusCode(500);
						failureRoutingContext.response().end(JsonUtils.toJson(new GenericMessageResponse("Internal error occured")));
					}
				}

			});

			coreRouters.put(ROOT_ROUTER_KEY, rootRouter);
		}
		return rootRouter;
	}

	private int getResponseStatusCode(Throwable failure) {
		if (failure instanceof EntityNotFoundException) {
			return 404;
		}
		if (failure instanceof InvalidPermissionException) {
			return 403;
		}
		if (failure instanceof HttpStatusCodeErrorException) {
			HttpStatusCodeErrorException error = (HttpStatusCodeErrorException) failure;
			return error.getCode();
		}
		return 500;

	}

	private void initAPIRouter() {
		Router router = getAPIRouter();
		router.route().handler(springConfiguration.corsHandler());
		router.route().handler(springConfiguration.bodyHandler());
		router.route().handler(CookieHandler.create());
		router.route().handler(springConfiguration.sessionHandler());
		router.route().handler(dataHandler);
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
	 * @return
	 * @throws InvalidNameException
	 */
	public Router addProjectRouter(String name) throws InvalidNameException {
		if (coreRouters.containsKey(name)) {
			throw new InvalidNameException(
					"The project name {"
							+ name
							+ "} is conflicting with a core router. Best guess is that an core verticle is already occupying the name. Please choose a different name or remove the conflicting core verticle.");
		}
		Router projectRouter = projectRouters.get(name);
		if (projectRouter == null) {
			projectRouter = Router.router(vertx);
			projectRouters.put(name, projectRouter);
			log.info("Added project router {" + name + "}");

			projectRouter.route().handler(ctx -> {
				ctx.data().put(PROJECT_CONTEXT_KEY, name);
				ctx.next();
			});

			getAPIRouter().mountSubRouter("/" + name, projectRouter);
			mountSubRoutersForProjectRouter(projectRouter, name);
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

}
