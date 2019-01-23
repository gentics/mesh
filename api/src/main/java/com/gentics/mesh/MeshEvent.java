package com.gentics.mesh;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.eventbus.EventBus;

/**
 * Central list of used eventbus addresses.
 */
public enum MeshEvent {

	MESH_MIGRATION("mesh.migration"),

	/**
	 * Event which is send once the mesh instance is fully started and ready to accept requests.
	 */
	STARTUP("mesh.startup-complete"),

	/**
	 * Address for handler which will process registered job.
	 */
	JOB_WORKER_ADDRESS("job.worker"),

	/**
	 * Event which is send once a new node is joining the cluster.
	 */
	CLUSTER_NODE_JOINING("mesh.cluster.node.joining"),

	/**
	 * Event which is send once a node finished joining the cluster.
	 */
	CLUSTER_NODE_JOINED("mesh.cluster.node.joined"),

	/**
	 * Event which is send once a node is about to leave the cluster.
	 */
	CLUSTER_NODE_LEAVING("mesh.cluster.node.leaving"),

	/**
	 * Event which is send once a node left the cluster.
	 */
	CLUSTER_NODE_LEFT("mesh.cluster.node.left"),

	/**
	 * Event which is send once the database status (offline, online, not_available, backup, synchronizing) changes.
	 */
	CLUSTER_DATABASE_CHANGE_STATUS("mesh.cluster.db.status"),

	/**
	 * Event which is send to update the permission stores.
	 */
	CLEAR_PERMISSION_STORE("mesh.clear-permission-store"),

	/* User */

	USER_CREATED("mesh.user.created"),

	USER_UPDATED("mesh.user.updated"),

	USER_DELETED("mesh.user.deleted"),

	/* Group */

	GROUP_CREATED("mesh.group.created"),

	GROUP_UPDATED("mesh.group.updated"),

	GROUP_DELETED("mesh.group.deleted"),

	/* Role */

	ROLE_CREATED("mesh.role.created"),

	ROLE_UPDATED("mesh.role.updated"),

	ROLE_DELETED("mesh.role.deleted"),

	/* Tag */

	TAG_CREATED("mesh.tag.created"),

	TAG_UPDATED("mesh.tag.updated"),

	TAG_DELETED("mesh.tag.deleted"),

	/* Tag Family */

	TAG_FAMILY_CREATED("mesh.tagfamily.created"),

	TAG_FAMILY_UPDATED("mesh.tagfamily.updated"),

	TAG_FAMILY_DELETED("mesh.tagfamily.deleted"),

	/* Project */

	PROJECT_CREATED("mesh.project.created"),

	PROJECT_UPDATED("mesh.project.updated"),

	PROJECT_DELETED("mesh.project.deleted"),

	/* Node */

	NODE_CREATED("mesh.node.created"),

	NODE_UPDATED("mesh.node.updated"),

	NODE_DELETED("mesh.node.deleted"),

	/* Schema */

	SCHEMA_CREATED("mesh.schema.created"),

	SCHEMA_UPDATED("mesh.schema.updated"),

	SCHEMA_DELETED("mesh.schema.deleted"),

	/* Microschema */

	MICROSCHEMA_CREATED("mesh.microschema.created"),

	MICROSCHEMA_UPDATED("mesh.microschema.updated"),

	MICROSCHEMA_DELETED("mesh.microschema.deleted"),

	/* Branch */

	BRANCH_CREATED("mesh.branch.created"),

	BRANCH_UPDATED("mesh.branch.updated"),

	BRANCH_DELETED("mesh.branch.deleted"),

	/* Search index related */

	/**
	 * Address for the handler which will process index sync requests.
	 */
	INDEX_SYNC_WORKER_ADDRESS("index-sync.worker"),

	/**
	 * Address to which index sync results will be published (failed, succeeded)
	 */
	INDEX_SYNC("mesh.search.index.sync");


	public final String address;

	MeshEvent(String address) {
		this.address = address;
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

}
