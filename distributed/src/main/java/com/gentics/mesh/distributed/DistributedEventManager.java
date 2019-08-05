package com.gentics.mesh.distributed;

import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_PERMISSION_STORE;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_LEFT;
import static com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS.ONLINE;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.cache.PermissionStore;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.router.RouterStorage;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;

import dagger.Lazy;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * The distributed event manager is responsible for handling cluster specific events.
 */
@Singleton
public class DistributedEventManager {

	private static Logger log = LoggerFactory.getLogger(DistributedEventManager.class);

	private final Lazy<Vertx> vertx;

	private final Lazy<Database> db;

	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public DistributedEventManager(Lazy<Vertx> vertx, Lazy<Database> db, Lazy<BootstrapInitializer> boot) {
		this.vertx = vertx;
		this.db = db;
		this.boot = boot;
	}

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
			PermissionStore.invalidate(vertx.get(), false);
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
		PermissionStore.invalidate(vertx.get());
	}

	private void synchronizeProjectRoutes() throws InvalidNameException {
		BootstrapInitializer cboot = boot.get();
		Database cdb = db.get();

		try (Tx tx = cdb.tx()) {
			for (RouterStorage rs : RouterStorage.getInstances()) {
				Map<String, Router> registeredProjectRouters = rs.root().apiRouter().projectsRouter().getProjectRouters();
				// Load all projects and check whether they are already registered
				for (Project project : cboot.meshRoot().getProjectRoot().findAll()) {
					if (registeredProjectRouters.containsKey(project.getName())) {
						continue;
					} else {
						rs.root().apiRouter().projectsRouter().addProjectRouter(project.getName());
					}
				}
			}
		}
	}
}
