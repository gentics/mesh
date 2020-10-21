package com.gentics.mesh.changelog.highlevel;

import com.gentics.mesh.cli.PostProcessFlags;
import com.gentics.mesh.core.data.changelog.HighLevelChange;
import com.gentics.mesh.core.data.root.MeshRoot;

public interface HighLevelChangelogSystem {
	/**
	 * Apply the changes which were not yet applied.
	 *
	 * @param meshRoot
	 * @return 
	 */
	void apply(PostProcessFlags flags, MeshRoot meshRoot);

	/**
	 * Check whether the change has already been applied.
	 *
	 * @param root
	 * @param change
	 * @return
	 */
	default boolean isApplied(MeshRoot root, HighLevelChange change) {
		return root.getChangelogRoot().hasChange(change);
	}

	/**
	 * Mark all high level changes as applied.
	 *
	 * @param meshRoot
	 */
	void markAllAsApplied(MeshRoot meshRoot);


	/**
	 * Check whether any high level changelog entry needs to be applied.
	 * 
	 * @param meshRoot
	 * @return
	 */
	boolean requiresChanges(MeshRoot meshRoot);
}
