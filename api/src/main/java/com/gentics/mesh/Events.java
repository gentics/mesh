package com.gentics.mesh;

/**
 * Central list of used eventbus addresses.
 */
public final class Events {

	public static final String MESH_MIGRATION = "mesh.migration";

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
	public static final String EVENT_CLUSTER_CLEAR_PERMISSIONS = "mesh.cluster.clear-permission-store";

}
