package com.gentics.mesh.router;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.URIUtils.encodeSegment;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.HashMap;
import java.util.Map;

import javax.naming.InvalidNameException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * Router for all projects (e.g.: :apibase:/demo) The project router {@link ProjectRouter} will later contain the actual project specific endpoints. (e.g.:
 * :apibase:/demo/nodes)
 */
public class ProjectsRouter {

	private static final Logger log = LoggerFactory.getLogger(ProjectsRouter.class);

	private ProjectRouter projectRouter;

	public static final String PROJECT_CONTEXT_KEY = "mesh-project";

	/**
	 * Project routers are routers that handle project rest api endpoints. E.g: :apibase:/dummy, :apibase:/yourprojectname
	 */
	private Map<String, Router> projectRouters = new HashMap<>();

	private final Vertx vertx;

	private APIRouter apiRouter;

	private Router router;


	public ProjectsRouter(Vertx vertx, APIRouter apiRouter) {
		this.vertx = vertx;
		this.apiRouter = apiRouter;
		this.router = Router.router(vertx);
		this.projectRouter = new ProjectRouter(vertx, apiRouter.getRoot().getStorage());
		apiRouter.getRouter().mountSubRouter("/", router);
	}

	/**
	 * Fail if the provided name is invalid or would cause a conflicts with an existing API router.
	 * 
	 * @param name
	 *            Project name to be checked
	 */
	public void assertProjectNameValid(String name) {
		String encodedName = encodeSegment(name);
		Map<String, Router> apiRouters = apiRouter.getRouters();
		if (apiRouters.containsKey(name) || apiRouters.containsKey(encodedName)) {
			throw error(BAD_REQUEST, "project_error_name_already_reserved", name);
		}
	}

	/**
	 * Add a new project router with the given name to the projects router. This method will return an existing router when one already has been setup.
	 * 
	 * @param name
	 *            Name of the project router
	 * @return Router for the given project name
	 * @throws InvalidNameException
	 */
	public Router addProjectRouter(String name) throws InvalidNameException {
		String encodedName = encodeSegment(name);
		assertProjectNameValid(name);
		Router projectRouter = projectRouters.get(name);
		if (projectRouter == null) {
			projectRouter = Router.router(vertx);

			projectRouters.put(name, projectRouter);
			log.info("Added project router {" + name + "}");

			projectRouter.route().handler(ctx -> {
				BootstrapInitializer boot = apiRouter.getRoot().getStorage().getBoot().get();
				Database db = apiRouter.getRoot().getStorage().getDb().get();
				Project project = db.tx(() -> boot.projectRoot().findByName(name));
				if (project == null) {
					log.warn("Project for name {" + name + "} could not be found.");
					ctx.fail(error(NOT_FOUND, "project_not_found", name));
					return;
				}
				ctx.data().put(ProjectsRouter.PROJECT_CONTEXT_KEY, project);
				ctx.next();
			});
			router.mountSubRouter("/" + encodedName, projectRouter);
			projectRouter.mountSubRouter("/", this.projectRouter.getRouter());
			// mountSubRoutersForProjectRouter(projectRouter, encodedName);
		}
		return projectRouter;
	}

	// /**
	// * Mount all registered project subrouters on the project router.
	// *
	// * @param projectRouter
	// * @param projectRouterName
	// * Name of the project router
	// */
	// private void mountSubRoutersForProjectRouter(Router projectRouter, String projectRouterName) {
	// for (Entry<String, Router> entry : this.projectRouter.getRouters().entrySet()) {
	// String mountPoint = entry.getKey();
	// Router subRouter = entry.getValue();
	// log.info("Mounting subrouter {" + mountPoint + "} onto given project router for project {" + projectRouterName + "}");
	// projectRouter.mountSubRouter("/" + mountPoint, subRouter);
	// }
	// }

	/**
	 * Check whether the project router for the given project name is already registered.
	 * 
	 * @param projectName
	 * @return
	 */
	public boolean hasProjectRouter(String projectName) {
		return projectRouters.containsKey(projectName);
	}

	public Map<String, Router> getProjectRouters() {
		return projectRouters;
	}

	/**
	 * Common router which holds project specific routes (e.g: /nodes /tagFamilies)
	 * 
	 * @return
	 */
	public ProjectRouter projectRouter() {
		return projectRouter;
	}

}
