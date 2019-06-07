package com.gentics.mesh.graphdb.cluster;

import java.io.IOException;

import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;

public interface ClusterManager {

	/**
	 * Join the cluster and block until the graph database has been received.
	 * 
	 * @throws InterruptedException
	 */
	void joinCluster() throws InterruptedException;

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
	void startServer() throws Exception;

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

	void stop();

	/**
	 * Register event handlers which are used to invoke operations on the database server.
	 */
	void registerEventHandlers();

}
