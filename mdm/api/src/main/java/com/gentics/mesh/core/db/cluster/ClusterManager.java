package com.gentics.mesh.core.db.cluster;

import java.io.IOException;

import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;

import io.reactivex.Completable;

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

	/**
	 * Returns a completable which will complete once the quorum has been reached.
	 * 
	 * @return
	 */
	Completable waitUntilWriteQuorumReached();

	/**
	 * Checks if the cluster storage is locked cluster-wide.
	 * 
	 * @return
	 */
	boolean isClusterTopologyLocked();

	/**
	 * Checks whether the local node is online and fully usable.
	 * @return online status
	 */
	boolean isLocalNodeOnline();

	/**
	 * Checks if write quorum is reached
	 *
	 * @return
	 */
	boolean isWriteQuorumReached();
}
