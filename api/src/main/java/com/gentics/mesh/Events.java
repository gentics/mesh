package com.gentics.mesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Central list of used eventbus addresses.
 */
public final class Events {

	public static final String MESH_MIGRATION = "mesh.migration";

	/**
	 * Event which is send once the mesh instance is fully started and ready to accept requests.
	 */
	public static final String STARTUP_EVENT_ADDRESS = "mesh.startup-complete";

	/**
	 * Address for handler which will process registered job.
	 */
	public final static String JOB_WORKER_ADDRESS = "job.worker";

	/**
	 * Event which is send once a new node is joining the cluster.
	 */
	public static final String EVENT_CLUSTER_NODE_JOINING = "mesh.cluster.node.joining";

	/**
	 * Event which is send once a node finished joining the cluster.
	 */
	public static final String EVENT_CLUSTER_NODE_JOINED = "mesh.cluster.node.joined";

	/**
	 * Event which is send once a node left the cluster.
	 */
	public static final String EVENT_CLUSTER_NODE_LEFT = "mesh.cluster.node.left";

	/**
	 * Event which is send once the database status (offline, online, not_available, backup, synchronizing) changes.
	 */
	public static final String EVENT_CLUSTER_DATABASE_CHANGE_STATUS = "mesh.cluster.db.status";

	/**
	 * Event which is send to update the permission stores.
	 */
	public static final String EVENT_CLEAR_PERMISSION_STORE = "mesh.clear-permission-store";

	/* User */

	public static final String EVENT_USER_CREATED = "mesh.user.created";

	public static final String EVENT_USER_UPDATED = "mesh.user.updated";

	public static final String EVENT_USER_DELETED = "mesh.user.deleted";

	/* Group */

	public static final String EVENT_GROUP_CREATED = "mesh.group.created";

	public static final String EVENT_GROUP_UPDATED = "mesh.group.updated";

	public static final String EVENT_GROUP_DELETED = "mesh.group.deleted";

	/* Role */

	public static final String EVENT_ROLE_CREATED = "mesh.role.created";

	public static final String EVENT_ROLE_UPDATED = "mesh.role.updated";

	public static final String EVENT_ROLE_DELETED = "mesh.role.deleted";

	/* Tag */

	public static final String EVENT_TAG_CREATED = "mesh.tag.created";

	public static final String EVENT_TAG_UPDATED = "mesh.tag.updated";

	public static final String EVENT_TAG_DELETED = "mesh.tag.deleted";

	/* Tag */

	public static final String EVENT_TAG_FAMILY_CREATED = "mesh.tagfamily.created";

	public static final String EVENT_TAG_FAMILY_UPDATED = "mesh.tagfamily.updated";

	public static final String EVENT_TAG_FAMILY_DELETED = "mesh.tagfamily.deleted";

	/* Project */

	public static final String EVENT_PROJECT_CREATED = "mesh.project.created";

	public static final String EVENT_PROJECT_UPDATED = "mesh.project.updated";

	public static final String EVENT_PROJECT_DELETED = "mesh.project.deleted";

	/* Node */

	public static final String EVENT_NODE_CREATED = "mesh.node.created";

	public static final String EVENT_NODE_UPDATED = "mesh.node.updated";

	public static final String EVENT_NODE_DELETED = "mesh.node.deleted";

	/* Schema */

	public static final String EVENT_SCHEMA_CREATED = "mesh.schema.created";

	public static final String EVENT_SCHEMA_UPDATED = "mesh.schema.updated";

	public static final String EVENT_SCHEMA_DELETED = "mesh.schema.deleted";

	/* Microschema */

	public static final String EVENT_MICROSCHEMA_CREATED = "mesh.microschema.created";

	public static final String EVENT_MICROSCHEMA_UPDATED = "mesh.microschema.updated";

	public static final String EVENT_MICROSCHEMA_DELETED = "mesh.microschema.deleted";

	/* Release */

	public static final String EVENT_RELEASE_CREATED = "mesh.release.created";

	public static final String EVENT_RELEASE_UPDATED = "mesh.release.updated";

	public static final String EVENT_RELEASE_DELETED = "mesh.release.deleted";

	/**
	 * Returns a list of all events which are publically exposed via the eventbus websocket bridge.
	 * 
	 * @return
	 */
	public static List<String> publicEvents() {
		List<String> events = new ArrayList<>();

		events.add(MESH_MIGRATION);

		events.add(STARTUP_EVENT_ADDRESS);

		events.add(EVENT_CLUSTER_NODE_JOINING);

		events.add(EVENT_CLUSTER_NODE_JOINED);

		events.add(EVENT_CLUSTER_NODE_LEFT);

		events.add(EVENT_CLUSTER_DATABASE_CHANGE_STATUS);
		events.add(EVENT_CLEAR_PERMISSION_STORE);

		/* User */

		events.add(EVENT_USER_CREATED);

		events.add(EVENT_USER_UPDATED);

		events.add(EVENT_USER_DELETED);

		/* Group */

		events.add(EVENT_GROUP_CREATED);

		events.add(EVENT_GROUP_UPDATED);

		events.add(EVENT_GROUP_DELETED);

		/* Role */

		events.add(EVENT_ROLE_CREATED);

		events.add(EVENT_ROLE_UPDATED);

		events.add(EVENT_ROLE_DELETED);

		/* Tag */

		events.add(EVENT_TAG_CREATED);

		events.add(EVENT_TAG_UPDATED);

		events.add(EVENT_TAG_DELETED);

		/* Tag */

		events.add(EVENT_TAG_FAMILY_CREATED);

		events.add(EVENT_TAG_FAMILY_UPDATED);

		events.add(EVENT_TAG_FAMILY_DELETED);

		/* Project */

		events.add(EVENT_PROJECT_CREATED);

		events.add(EVENT_PROJECT_UPDATED);

		events.add(EVENT_PROJECT_DELETED);

		/* Node */

		events.add(EVENT_NODE_CREATED);

		events.add(EVENT_NODE_UPDATED);

		events.add(EVENT_NODE_DELETED);

		/* Schema */

		events.add(EVENT_SCHEMA_CREATED);

		events.add(EVENT_SCHEMA_UPDATED);

		events.add(EVENT_SCHEMA_DELETED);

		/* Microschema */

		events.add(EVENT_MICROSCHEMA_CREATED);

		events.add(EVENT_MICROSCHEMA_UPDATED);

		events.add(EVENT_MICROSCHEMA_DELETED);

		/* Release */

		events.add(EVENT_RELEASE_CREATED);

		events.add(EVENT_RELEASE_UPDATED);

		events.add(EVENT_RELEASE_DELETED);

		return events;
	}

}
