package com.gentics.mesh.graphdb.cluster;

import java.io.IOException;

import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.hazelcast.core.HazelcastInstance;

import io.reactivex.Completable;

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
	void startAndSync() throws Exception;

	/**
	 * Stop the server and release all used resources.
	 */
	void stop();

	/**
	 * Return the hazelcast instance which was created by the manager.
	 * 
	 * @return
	 */
	HazelcastInstance getHazelcast();

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

	/**
	 * Returns a completable which will complete once the quorum has been reached.
	 * 
	 * @return
	 */
	Completable waitUntilWriteQuorumReached();

}
