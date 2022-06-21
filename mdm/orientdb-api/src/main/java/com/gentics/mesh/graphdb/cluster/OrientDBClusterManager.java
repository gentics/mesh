package com.gentics.mesh.graphdb.cluster;

import com.gentics.mesh.core.db.cluster.ClusterManager;

public interface OrientDBClusterManager extends ClusterManager {

	/**
	 * Start the graph database server which will provide cluster support.
	 *
	 * @throws Exception
	 */
	void startAndSync() throws Exception;

	/**
	 * Stop the server and release all used resources.
	 */
	void stop();
}
