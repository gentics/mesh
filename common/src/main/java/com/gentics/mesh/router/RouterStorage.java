package com.gentics.mesh.router;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.VersionHandler;

import dagger.Lazy;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;

/**
 * Central storage for all Vert.x web request routers.
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

	private final RootRouter rootRouter;

	private final Vertx vertx;

	private final MeshOptions options;

	private Lazy<BootstrapInitializer> boot;

	private Lazy<Database> db;

	public CorsHandler corsHandler;

	public BodyHandler bodyHandler;

	public final VersionHandler versionHandler;

	private MeshAuthChain authChain;

	private final RouterStorageRegistry routerStorageRegistry;

	@Inject
	public RouterStorage(Vertx vertx, MeshOptions options, MeshAuthChain authChain, CorsHandler corsHandler, BodyHandlerImpl bodyHandler,
		Lazy<BootstrapInitializer> boot,
		Lazy<Database> db, VersionHandler versionHandler, RouterStorageRegistry routerStorageRegistry) {
		this.vertx = vertx;
		this.options = options;
		this.boot = boot;
		this.db = db;
		this.corsHandler = corsHandler;
		this.bodyHandler = bodyHandler;
		this.authChain = authChain;
		this.versionHandler = versionHandler;
		this.routerStorageRegistry = routerStorageRegistry;

		// Initialize the router chain. The root router will create additional routers which will be mounted.
		rootRouter = new RootRouter(vertx, this, options);

		// TODO move this to the place where the routerstorage is created
		routerStorageRegistry.getInstances().add(this);
	}

	public void registerEventbusHandlers() {
		ProjectsRouter projectsRouter = rootRouter.apiRouter().projectsRouter();
		EventBus eb = vertx.eventBus();
		eb.consumer(PROJECT_CREATED.address, (Message<JsonObject> rh) -> {
			JsonObject json = rh.body();

			// Check whether this is a local message. We only need to react on foreign messages.
			// Local updates for project creation / deletion is already handled locally
			String origin = json.getString("origin");
			String nodeName = options.getNodeName();
			if (nodeName.equals(origin)) {
				rh.reply(true);
				return;
			}
			String name = json.getString("name");
			try {
				routerStorageRegistry.addProject(name);
				rh.reply(true);
				if (log.isInfoEnabled()) {
					log.info("Registered project {" + name + "}");
				}
			} catch (InvalidNameException e) {
				rh.fail(400, e.getMessage());
				throw error(BAD_REQUEST, "Error while adding project to router storage", e);
			}
		});

		eb.consumer(PROJECT_UPDATED.address, (Message<JsonObject> rh) -> {
			Database database = db.get();

			try (Tx tx = database.tx()) {
				// Check whether there are any projects which do not have an
				// active project router
				for (Project project : boot.get().projectRoot().findAll()) {
					if (!projectsRouter.hasProjectRouter(project.getName())) {
						log.info("Mounting project {" + project.getName() + "}");
						projectsRouter.addProjectRouter(project.getName());
					}
				}
			} catch (InvalidNameException e) {
				log.error("Could not update project routers", e);
				rh.fail(400, "Invalid project name found");
			}

		});

	}

	public Lazy<Database> getDb() {
		return db;
	}

	public Lazy<BootstrapInitializer> getBoot() {
		return boot;
	}

	public MeshAuthChain getAuthChain() {
		return authChain;
	}

	public RootRouter root() {
		return rootRouter;
	}

}
