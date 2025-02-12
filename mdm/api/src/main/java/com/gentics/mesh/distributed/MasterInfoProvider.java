package com.gentics.mesh.distributed;

/**
 * Interface for an implementation which determines whether the current instance is the elected master in the cluster
 */
public interface MasterInfoProvider {
	/**
	 * Returns true when this instance is the master
	 * @return true for master
	 */
	boolean isMaster();
}
