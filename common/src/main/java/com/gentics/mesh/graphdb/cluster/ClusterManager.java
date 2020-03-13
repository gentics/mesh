package com.gentics.mesh.graphdb.cluster;

import java.io.IOException;

import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;

public interface ClusterManager {

	/**
	 * Initialize the configuration files.
	 * 
	 * @throws IOException
	 */
	void initConfigurationFiles() throws IOException;

	/**
	 * Start the graph database server which will provide cluster support.
	 * 
	 * @throws Exception
	 */
	void start() throws Exception;

	/**
	 * Stop the server and release all used resources.
	 */
	void stop();

	/**
	 * Return the hazelcast instance which was started by the graph database server.
	 * 
	 * @return
	 */
	Object getHazelcast();

	/**
	 * Return the database cluster status.
	 * 
	 * @return
	 */
	ClusterStatusResponse getClusterStatus();

	/**
	 * Register event handlers which are used to invoke operations on the database server.
	 */
	void registerEventHandlers();

}
