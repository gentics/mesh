package com.gentics.mesh;

/**
 * Central list of used eventbus addresses.
 */
public final class Events {

	public static final String MESH_MIGRATION = "mesh.migration";

	/**
	 * Event which is send once the mesh instance is fully started and ready to accept requests.
	 */
	public static final String STARTUP_EVENT_ADDRESS = "mesh.startup-complete";

	public final static String SCHEMA_MIGRATION_ADDRESS = "mesh.migration.schema";

	public final static String MICROSCHEMA_MIGRATION_ADDRESS = "mesh.migration.microschema";

	public final static String RELEASE_MIGRATION_ADDRESS = "mesh.migration.release";

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
	 * Event which is send once the project information has been updated.
	 */
	public static final String EVENT_CLUSTER_UPDATE_PROJECTS = "mesh.cluster.update-projects";

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

}
