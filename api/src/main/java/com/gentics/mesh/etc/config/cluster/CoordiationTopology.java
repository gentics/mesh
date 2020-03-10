package com.gentics.mesh.etc.config.cluster;

public enum CoordiationTopology {

	/**
	 * Don't manage cluster topology.
	 */
	UNMANAGED,

	/**
	 * Use the elected master also as database master and make other nodes in the cluster replicas.
	 */
	MASTER_REPLICA;

}
