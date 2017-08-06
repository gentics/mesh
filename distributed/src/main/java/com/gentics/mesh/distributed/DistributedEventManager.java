package com.gentics.mesh.distributed;

import static com.gentics.mesh.Events.EVENT_CLEAR_PERMISSION_STORE;
import static com.gentics.mesh.Events.EVENT_CLUSTER_NODE_JOINED;
import static com.gentics.mesh.Events.EVENT_CLUSTER_NODE_LEFT;
import static com.gentics.mesh.Events.EVENT_CLUSTER_UPDATE_PROJECTS;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.cache.PermissionStore;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.etc.RouterStorage;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * The distributed event manager is responsible for handling cluster specific events.
 */
@Singleton
public class DistributedEventManager {

	private static Logger log = LoggerFactory.getLogger(DistributedEventManager.class);

	@Inject
	public RouterStorage routerStorage;

	@Inject
	public BootstrapInitializer boot;

	@Inject
	public DistributedEventManager() {
	}

	public void registerHandlers() {
		EventBus eb = Mesh.mesh().getVertx().eventBus();

		// Register for events which indicate that the cluster topology changes
		eb.consumer(EVENT_CLUSTER_NODE_JOINED, handler -> {
			handleClusterTopologyUpdate(handler);
		});
		eb.consumer(EVENT_CLUSTER_NODE_LEFT, handler -> {
			handleClusterTopologyUpdate(handler);
		});

//		// Register for events which are send whenever a project is created or deleted somewhere in the cluster.
//		eb.consumer(EVENT_CLUSTER_UPDATE_PROJECTS, handler -> {
//			try {
//				synchronizeProjectRoutes();
//			} catch (Exception e) {
//				log.error("Error while handling event {" + EVENT_CLUSTER_UPDATE_PROJECTS + "}");
//				handler.fail(400, "Could not initialize prjects.");
//			}
//		});

		// Register for events which are send whenever the permission store must be invalidated.
		eb.consumer(EVENT_CLEAR_PERMISSION_STORE, handler -> {
			PermissionStore.invalidate();
		});
	}

	private void handleClusterTopologyUpdate(Message<Object> handler) {
		try {
			synchronizeProjectRoutes();
		} catch (Exception e) {
			log.error("Error while handling synchronizing projects during topology update event.");
			handler.fail(400, "Could not initialize projects.");
		}
		// Invalidate permission store since the permissions may have changed
		PermissionStore.invalidate();
	}

	private void synchronizeProjectRoutes() throws InvalidNameException {
		Map<String, Router> registeredProjectRouters = routerStorage.getProjectRouters();

		// Load all projects and check whether they are already registered
		for (Project project : boot.meshRoot().getProjectRoot().findAll()) {
			if (registeredProjectRouters.containsKey(project.getName())) {
				continue;
			} else {
				routerStorage.addProjectRouter(project.getName());
			}
		}

		// Check whether all registered projects are still existing
		for (String registeredProjectName : registeredProjectRouters.keySet()) {
			Project foundProject = boot.meshRoot().getProjectRoot().findByName(registeredProjectName);
			// The project was not found. The project router must be removed.
			if (foundProject == null) {
				routerStorage.removeProjectRouter(registeredProjectName);
			}
		}
	}
}
