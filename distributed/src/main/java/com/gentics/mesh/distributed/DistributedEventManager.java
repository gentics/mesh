package com.gentics.mesh.distributed;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.graphdb.cluster.TopologyEventBridge;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;
import dagger.Lazy;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;
import java.util.Map;

import static com.gentics.mesh.core.rest.MeshEvent.*;
import static com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS.ONLINE;

/**
 * The distributed event manager is responsible for handling cluster specific events.
 */
@Singleton
public class DistributedEventManager {

	private static Logger log = LoggerFactory.getLogger(DistributedEventManager.class);

	private final Lazy<Vertx> vertx;

	private final Lazy<Database> db;

	private final Lazy<BootstrapInitializer> boot;

	private final RouterStorageRegistryImpl routerStorageRegistry;

	private final Lazy<PermissionCache> permCache;

	@Inject
	public DistributedEventManager(Lazy<Vertx> vertx, Lazy<Database> db, Lazy<BootstrapInitializer> boot,
		RouterStorageRegistryImpl routerStorageRegistry,
		Lazy<PermissionCache> permCache) {
		this.vertx = vertx;
		this.db = db;
		this.boot = boot;
		this.routerStorageRegistry = routerStorageRegistry;
		this.permCache = permCache;
	}

	/**
	 * Handle events which were dispatched by the {@link TopologyEventBridge}.
	 */
	public void registerHandlers() {
		EventBus eb = vertx.get().eventBus();

		// Register for events which indicate that the cluster topology changes
		eb.consumer(CLUSTER_NODE_JOINED.address, handler -> {
			log.info("Received node joined event. Updating content structure information");
			handleClusterTopologyUpdate(handler);
		});
		eb.consumer(CLUSTER_NODE_LEFT.address, handler -> {
			log.info("Received node left event. Updating content structure information");
			handleClusterTopologyUpdate(handler);
		});

		// Register for events which are send whenever the permission store must be invalidated.
		eb.consumer(CLEAR_PERMISSION_STORE.address, handler -> {
			log.debug("Received permissionstore clear event");
			permCache.get().clear(false);
		});

		// React on project creates
		eb.consumer(Project.TYPE_INFO.getOnCreated().getAddress(), (Message<JsonObject> handler) -> {
			log.info("Received project create event");
			handleClusterTopologyUpdate(handler);
		});

		// React on project updates
		eb.consumer(Project.TYPE_INFO.getOnUpdated().getAddress(), (Message<JsonObject> handler) -> {
			log.info("Received project update event.");
			handleClusterTopologyUpdate(handler);
		});

		eb.consumer(CLUSTER_DATABASE_CHANGE_STATUS.address, (Message<JsonObject> handler) -> {
			JsonObject info = handler.body();
			String node = info.getString("node");
			String db = info.getString("database");
			DB_STATUS status = DB_STATUS.valueOf(info.getString("status"));
			log.info("Received status update from node {" + node + ":" + db + "} - " + status.name());
			if (ONLINE == status) {
				handleClusterTopologyUpdate(handler);
			}
		});
	}

	private void handleClusterTopologyUpdate(Message<?> handler) {
		if (log.isDebugEnabled()) {
			log.debug("Synchronizing the project routers");
		}
		try {
			synchronizeProjectRoutes();
		} catch (Exception e) {
			log.error("Error while handling synchronizing projects during topology update event.", e);
			handler.fail(400, "Could not initialize projects.");
		}
		// Invalidate permission store since the permissions may have changed
		permCache.get().clear();
	}

	private void synchronizeProjectRoutes() throws InvalidNameException {
		BootstrapInitializer cboot = boot.get();
		Database cdb = db.get();

		cdb.tx(tx -> {
			for (RouterStorage rs : routerStorageRegistry.getInstances()) {
				Map<String, Router> registeredProjectRouters = rs.root().apiRouter().projectsRouter().getProjectRouters();
				// Load all projects and check whether they are already registered
				for (HibProject project : cboot.projectDao().findAll()) {
					if (registeredProjectRouters.containsKey(project.getName())) {
						continue;
					} else {
						rs.root().apiRouter().projectsRouter().addProjectRouter(project.getName());
					}
				}
			}
		});
	}
}
