package com.gentics.mesh.core.data.changelog;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * High level change which can be applied once the low level changelog entries have been processed.
 */
public interface HighLevelChange extends Change {
	/**
	 * Check whether it is allowed to apply the changelog in a cluster.
	 * This should only return true for changes, that will not cause any problems if applied to a node in the cluster (which already uses the new Mesh version), while other nodes in the cluster
	 * still use the old Mesh version.
	 * @param options options
	 * @return true iff the changelog is allowed to be applied in a cluster.
	 */
	boolean isAllowedInCluster(MeshOptions options);
}
