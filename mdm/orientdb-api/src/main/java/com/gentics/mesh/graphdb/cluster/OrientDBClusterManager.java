package com.gentics.mesh.graphdb.cluster;

import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.hazelcast.core.HazelcastInstance;

public interface OrientDBClusterManager extends ClusterManager {

	/**
	 * Return the hazelcast instance which was created by the manager.
	 *
	 * @return
	 */
	HazelcastInstance getHazelcast();

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
