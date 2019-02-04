package com.gentics.mesh.core.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.event.CreatedMeshEventModel;
import com.gentics.mesh.core.rest.event.DeletedMeshEventModel;
import com.gentics.mesh.core.rest.event.UpdatedMeshEventModel;
import io.vertx.core.eventbus.EventBus;

/**
 * Central list of used eventbus addresses.
 */
public enum MeshEvent {

	MESH_MIGRATION("mesh.migration", null),

	/**
	 * Event which is send once the mesh instance is fully started and ready to accept requests.
	 */
	STARTUP("mesh.startup-complete", null),

	/**
	 * Address for handler which will process registered job.
	 */
	JOB_WORKER_ADDRESS("job.worker", null),

	/**
	 * Event which is send once a new node is joining the cluster.
	 */
	CLUSTER_NODE_JOINING("mesh.cluster.node.joining", null),

	/**
	 * Event which is send once a node finished joining the cluster.
	 */
	CLUSTER_NODE_JOINED("mesh.cluster.node.joined", null),

	/**
	 * Event which is send once a node is about to leave the cluster.
	 */
	CLUSTER_NODE_LEAVING("mesh.cluster.node.leaving", null),

	/**
	 * Event which is send once a node left the cluster.
	 */
	CLUSTER_NODE_LEFT("mesh.cluster.node.left", null),

	/**
	 * Event which is send once the database status (offline, online, not_available, backup, synchronizing) changes.
	 */
	CLUSTER_DATABASE_CHANGE_STATUS("mesh.cluster.db.status", null),

	/**
	 * Event which is send to update the permission stores.
	 */
	CLEAR_PERMISSION_STORE("mesh.clear-permission-store", null),

	/* User */

	USER_CREATED("mesh.user.created", CreatedMeshEventModel.class),

	USER_UPDATED("mesh.user.updated", UpdatedMeshEventModel.class),

	USER_DELETED("mesh.user.deleted", DeletedMeshEventModel.class),

	/* Group */

	GROUP_CREATED("mesh.group.created", CreatedMeshEventModel.class),

	GROUP_UPDATED("mesh.group.updated", UpdatedMeshEventModel.class),

	GROUP_DELETED("mesh.group.deleted", DeletedMeshEventModel.class),

	/* Role */

	ROLE_CREATED("mesh.role.created", CreatedMeshEventModel.class),

	ROLE_UPDATED("mesh.role.updated", UpdatedMeshEventModel.class),

	ROLE_DELETED("mesh.role.deleted", DeletedMeshEventModel.class),

	/* Tag */

	TAG_CREATED("mesh.tag.created", null),

	TAG_UPDATED("mesh.tag.updated", null),

	TAG_DELETED("mesh.tag.deleted", null),

	/* Tag Family */

	TAG_FAMILY_CREATED("mesh.tagfamily.created", null),

	TAG_FAMILY_UPDATED("mesh.tagfamily.updated", null),

	TAG_FAMILY_DELETED("mesh.tagfamily.deleted", null),

	/* Project */

	PROJECT_CREATED("mesh.project.created", null),

	PROJECT_UPDATED("mesh.project.updated", null),

	PROJECT_DELETED("mesh.project.deleted", null),

	/* Node */

	NODE_CREATED("mesh.node.created", null),

	NODE_UPDATED("mesh.node.updated", null),

	NODE_DELETED("mesh.node.deleted", null),

	/* Schema */

	SCHEMA_CREATED("mesh.schema.created", null),

	SCHEMA_UPDATED("mesh.schema.updated", null),

	SCHEMA_DELETED("mesh.schema.deleted", null),

	/* Microschema */

	MICROSCHEMA_CREATED("mesh.microschema.created", null),

	MICROSCHEMA_UPDATED("mesh.microschema.updated", null),

	MICROSCHEMA_DELETED("mesh.microschema.deleted", null),

	/* Branch */

	BRANCH_CREATED("mesh.branch.created", null),

	BRANCH_UPDATED("mesh.branch.updated", null),

	BRANCH_DELETED("mesh.branch.deleted", null),

	/* Search index related */

	/**
	 * Address for the handler which will process index sync requests.
	 */
	INDEX_SYNC_WORKER_ADDRESS("index-sync.worker", null),

	/**
	 * Address to which index sync results will be published (failed, succeeded)
	 */
	INDEX_SYNC("mesh.search.index.sync", null),

	/**
	 * Event that is emitted when the search verticle has been working and is now idle.
	 */
	SEARCH_IDLE("mesh.search.process.idle", null);

	public final String address;
	public final Class bodyModel;

	private static final Map<String, MeshEvent> events = createEventMap();

	/**
	 * Gets the event with the given address.
	 * Returns an empty optional if the address is invalid.
	 *
	 * @param address
	 * @return
	 */
	public static Optional<MeshEvent> fromAddress(String address) {
		return Optional.ofNullable(events.get(address));
	}

	private static Map<String, MeshEvent> createEventMap() {
		return Stream.of(values())
			.collect(Collectors.toMap(
				MeshEvent::getAddress,
				Function.identity()
			));
	}

	MeshEvent(String address, Class bodyModel) {
		this.address = address;
		this.bodyModel = bodyModel;
	}

	@Override
	public String toString() {
		return address;
	}

	// /**
	// * Address to query the index sync status.
	// */
	// INDEX_SYNC_STATUS_EVENT("mesh.search.index.sync.status"),

	public static void triggerJobWorker() {
		Mesh mesh = Mesh.mesh();
		EventBus eb = mesh.getVertx().eventBus();
		String name = mesh.getOptions().getNodeName();
		eb.send(JOB_WORKER_ADDRESS + name, null);
	}

	/**
	 * Returns a list of all events which are publicly exposed via the eventbus websocket bridge.
	 * 
	 * @return
	 */
	public static List<MeshEvent> publicEvents() {
		List<MeshEvent> events = new ArrayList<>();

		events.add(MESH_MIGRATION);

		events.add(STARTUP);

		events.add(CLUSTER_NODE_JOINING);

		events.add(CLUSTER_NODE_JOINED);

		events.add(CLUSTER_NODE_LEFT);

		events.add(CLUSTER_DATABASE_CHANGE_STATUS);
		events.add(CLEAR_PERMISSION_STORE);

		/* User */

		events.add(USER_CREATED);

		events.add(USER_UPDATED);

		events.add(USER_DELETED);

		/* Group */

		events.add(GROUP_CREATED);

		events.add(GROUP_UPDATED);

		events.add(GROUP_DELETED);

		/* Role */

		events.add(ROLE_CREATED);

		events.add(ROLE_UPDATED);

		events.add(ROLE_DELETED);

		/* Tag */

		events.add(TAG_CREATED);

		events.add(TAG_UPDATED);

		events.add(TAG_DELETED);

		/* Tag */

		events.add(TAG_FAMILY_CREATED);

		events.add(TAG_FAMILY_UPDATED);

		events.add(TAG_FAMILY_DELETED);

		/* Project */

		events.add(PROJECT_CREATED);

		events.add(PROJECT_UPDATED);

		events.add(PROJECT_DELETED);

		/* Node */

		events.add(NODE_CREATED);

		events.add(NODE_UPDATED);

		events.add(NODE_DELETED);

		/* Schema */

		events.add(SCHEMA_CREATED);

		events.add(SCHEMA_UPDATED);

		events.add(SCHEMA_DELETED);

		/* Microschema */

		events.add(MICROSCHEMA_CREATED);

		events.add(MICROSCHEMA_UPDATED);

		events.add(MICROSCHEMA_DELETED);

		/* Branch */

		events.add(BRANCH_CREATED);

		events.add(BRANCH_UPDATED);

		events.add(BRANCH_DELETED);

		/* Search Index */

		events.add(INDEX_SYNC);

		// events.add(INDEX_SYNC_STATUS_EVENT);

		return events;
	}

	public String getAddress() {
		return address;
	}
}
