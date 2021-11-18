package com.gentics.mesh.router;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.URIUtils.encodeSegment;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.HashMap;
import java.util.Map;

import javax.naming.InvalidNameException;

import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.shared.SharedKeys;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * Router for all projects (e.g.: :apibase:/demo) The project router {@link ProjectRouterImpl} will later contain the actual project specific endpoints. (e.g.:
 * :apibase:/demo/nodes)
 */
public class ProjectsRouterImpl implements ProjectsRouter {

	private static final Logger log = LoggerFactory.getLogger(ProjectsRouterImpl.class);

	private ProjectRouterImpl projectRouter;

	/**
	 * Project routers are routers that handle project rest api endpoints. E.g: :apibase:/dummy, :apibase:/yourprojectname
	 */
	private Map<String, Router> projectRouters = new HashMap<>();

	private final Vertx vertx;

	private APIRouterImpl apiRouter;

	private Router router;

	public ProjectsRouterImpl(Vertx vertx, APIRouterImpl apiRouter) {
		this.vertx = vertx;
		this.apiRouter = apiRouter;
		this.router = Router.router(vertx);
		this.projectRouter = new ProjectRouterImpl(vertx, apiRouter.getRoot().getStorage());
		apiRouter.getRouter().mountSubRouter("/", router);
	}

	@Override
	public void assertProjectNameValid(String name) {
		String encodedName = encodeSegment(name);
		Map<String, Router> apiRouters = apiRouter.getRouters();
		if (apiRouters.containsKey(name) || apiRouters.containsKey(encodedName)) {
			throw error(BAD_REQUEST, "project_error_name_already_reserved", name);
		}
	}

	@Override
	public Router addProjectRouter(String name) throws InvalidNameException {
		String encodedName = encodeSegment(name);
		assertProjectNameValid(name);
		Router projectRouter = projectRouters.get(name);
		if (projectRouter == null) {
			projectRouter = Router.router(vertx);

			projectRouters.put(name, projectRouter);
			log.info("Added project router {" + name + "}");

			projectRouter.route().handler(ctx -> {
				Database db = (Database) apiRouter.getRoot().getStorage().getDb();
				HibProject project = db.tx(tx -> {
					return tx.projectDao().findByName(name);
				});
				if (project == null) {
					log.warn("Project for name {" + name + "} could not be found.");
					ctx.fail(error(NOT_FOUND, "project_not_found", name));
					return;
				}
				ctx.put(SharedKeys.PROJECT_CONTEXT_KEY, project);
				ctx.next();
			});
			// Note: the end slash in the subrouter mount point is important, otherwise the subrouter for e.g. /project
			// (for a project named "project") would also match for the route /projects, which will cause problems,
			// if the project "project" is deleted
			router.mountSubRouter("/" + encodedName + "/", projectRouter);
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

	@Override
	public boolean hasProjectRouter(String projectName) {
		return projectRouters.containsKey(projectName);
	}

	@Override
	public Map<String, Router> getProjectRouters() {
		return projectRouters;
	}

	@Override
	public ProjectRouter projectRouter() {
		return projectRouter;
	}

}
