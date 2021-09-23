package com.gentics.mesh.router;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;
import javax.naming.InvalidNameException;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.GlueDatabase;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.distributed.TopologyChangeReadonlyHandler;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.handler.VersionHandlerImpl;
import com.gentics.mesh.monitor.liveness.LivenessManager;

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
 * @see RouterStorage
 */
public class RouterStorageImpl implements RouterStorage {

	private static final Logger log = LoggerFactory.getLogger(RouterStorageImpl.class);

	private final RootRouterImpl rootRouter;

	private final Vertx vertx;

	private final MeshOptions options;

	private Lazy<BootstrapInitializer> boot;

	private Lazy<Database> db;

	public CorsHandler corsHandler;

	public BodyHandler bodyHandler;

	public final VersionHandlerImpl versionHandler;

	private MeshAuthChainImpl authChain;

	private final RouterStorageRegistryImpl routerStorageRegistry;

	private final RequestDelegator delegator;

	private final TopologyChangeReadonlyHandler topologyChangeReadonlyHandler;

	@Inject
	public RouterStorageImpl(Vertx vertx, MeshOptions options, MeshAuthChainImpl authChain, CorsHandler corsHandler, BodyHandlerImpl bodyHandler,
		Lazy<BootstrapInitializer> boot,
		Lazy<Database> db, VersionHandlerImpl versionHandler,
		RouterStorageRegistryImpl routerStorageRegistry,
		RequestDelegator delegator, TopologyChangeReadonlyHandler topologyChangeReadonlyHandler, LivenessManager liveness) {
		this.vertx = vertx;
		this.options = options;
		this.boot = boot;
		this.db = db;
		this.corsHandler = corsHandler;
		this.bodyHandler = bodyHandler;
		this.authChain = authChain;
		this.versionHandler = versionHandler;
		this.routerStorageRegistry = routerStorageRegistry;
		this.delegator = delegator;
		this.topologyChangeReadonlyHandler = topologyChangeReadonlyHandler;

		// Initialize the router chain. The root router will create additional routers which will be mounted.
		rootRouter = new RootRouterImpl(vertx, this, options, liveness);

		// TODO move this to the place where the routerstorage is created
		routerStorageRegistry.getInstances().add(this);
	}

	@Override
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

			try {
				database.tx(tx -> {
					// Check whether there are any projects which do not have an
					// active project router
					for (HibProject project : tx.projectDao().findAll()) {
						if (!projectsRouter.hasProjectRouter(project.getName())) {
							log.info("Mounting project {" + project.getName() + "}");
							projectsRouter.addProjectRouter(project.getName());
						}
					}
				});
			} catch (RuntimeException e) {
				if (e.getCause() instanceof InvalidNameException) {
					log.error("Could not update project routers", e);
					rh.fail(400, "Invalid project name found");
				} else {
					throw e;
				}
			}

		});

	}

	@Override
	public GlueDatabase getDb() {
		return db.get();
	}

	public Lazy<BootstrapInitializer> getBoot() {
		return boot;
	}

	@Override
	public MeshAuthChain getAuthChain() {
		return authChain;
	}

	@Override
	public VersionHandler getVersionHandler() {
		return versionHandler;
	}

	@Override
	public BodyHandler getBodyHandler() {
		return bodyHandler;
	}

	@Override
	public RootRouter root() {
		return rootRouter;
	}

	@Override
	public CorsHandler getCorsHandler() {
		return corsHandler;
	}

	@Override
	public RequestDelegator getDelegator() {
		return delegator;
	}

	/**
	 * Get the topology change read-only handler
	 * @return handler
	 */
	@Override
	public TopologyChangeReadonlyHandler getTopologyChangeReadonlyHandler() {
		return topologyChangeReadonlyHandler;
	}
}
