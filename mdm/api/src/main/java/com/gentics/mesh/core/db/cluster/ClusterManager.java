package com.gentics.mesh.core.db.cluster;

import java.io.IOException;

import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.hazelcast.core.HazelcastInstance;

/**
 * The cluster manager provides cluster support for Gentics Mesh.
 */
public interface ClusterManager {

	/**
	 * Return the vertx cluster manager
	 * @return
	 */
	io.vertx.core.spi.cluster.ClusterManager getVertxClusterManager();

	/**
	 * Return the hazelcast instance which was created by the manager.
	 *
	 * @return
	 */
	HazelcastInstance getHazelcast();

	/**
	 * Initialize the configuration files.
	 * 
	 * @throws IOException
	 */
	void initConfigurationFiles() throws IOException;

	/**
	 * Return the database cluster status.
	 * 
	 * @return
	 */
	ClusterStatusResponse getClusterStatus();
}
