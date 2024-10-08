package com.gentics.mesh.distributed;

import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_CACHES;
import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_PERMISSION_STORE;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_LEFT;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

import com.gentics.mesh.cache.CacheRegistry;
import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.RouterStorageRegistryImpl;

import dagger.Lazy;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * The distributed event manager is responsible for handling cluster specific events.
 */
@Singleton
public class DistributedEventManager {

	private static Logger log = LoggerFactory.getLogger(DistributedEventManager.class);

	private final Lazy<Vertx> vertx;

	private final Lazy<Database> db;

	private final RouterStorageRegistryImpl routerStorageRegistry;

	private final Lazy<PermissionCache> permCache;

	private final CacheRegistry cacheRegistry;

	@Inject
	public DistributedEventManager(Lazy<Vertx> vertx, Lazy<Database> db, RouterStorageRegistryImpl routerStorageRegistry,
		Lazy<PermissionCache> permCache, CacheRegistry cacheRegistry) {
		this.vertx = vertx;
		this.db = db;
		this.routerStorageRegistry = routerStorageRegistry;
		this.permCache = permCache;
		this.cacheRegistry = cacheRegistry;
	}

	/**
	 * Handle events which were dispatched by the db-level topology change listener implementation.
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

		// Register for event to clear the caches
		eb.consumer(CLEAR_CACHES.address, handler -> {
			log.debug("Received cache clear event");
			cacheRegistry.clear();
		});

		// React on project creates
		eb.consumer(HibProject.TYPE_INFO.getOnCreated().getAddress(), (Message<JsonObject> handler) -> {
			log.info("Received project create event");
			handleClusterTopologyUpdate(handler);
		});

		// React on project updates
		eb.consumer(HibProject.TYPE_INFO.getOnUpdated().getAddress(), (Message<JsonObject> handler) -> {
			log.info("Received project update event.");
			handleClusterTopologyUpdate(handler);
		});

		eb.consumer(CLUSTER_DATABASE_CHANGE_STATUS.address, (Message<JsonObject> handler) -> {
			JsonObject info = handler.body();
			String node = info.getString("node");
			String db = info.getString("database");
			String statusText = info.getString("status");
			boolean online = info.getBoolean("online", false);
			log.info("Received status update from node {" + node + ":" + db + "} - " + statusText);
			if (online) {
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
		Database cdb = db.get();

		cdb.tx(tx -> {
			for (RouterStorage rs : routerStorageRegistry.getInstances()) {
				Map<String, Router> registeredProjectRouters = rs.root().apiRouter().projectsRouter().getProjectRouters();
				// Load all projects and check whether they are already registered
				for (HibProject project : tx.projectDao().findAll()) {
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
