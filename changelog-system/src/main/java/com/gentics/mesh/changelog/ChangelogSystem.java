package com.gentics.mesh.changelog;

import java.util.List;

import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.cli.PostProcessFlags;

public interface ChangelogSystem {
	/**
	 * Apply all listed changes.
	 *
	 * @param flags
	 * @param list
	 * @return Flag which indicates whether all changes were applied successfully
	 */
	boolean applyChanges(PostProcessFlags flags, List<Change> list);

	/**
	 * Check whether all changes have been applied or whether the changelog would need to apply changes.
	 * 
	 * @return
	 */
	boolean requiresChanges();

	/**
	 * Mark all changelog entries as applied. This is useful if you resolved issues manually or if you want to create a fresh mesh database dump.
	 */
	void markAllAsApplied(List<Change> list);

	/**
	 * Apply all changes from the {@link ChangesList}.
	 *
	 * @param flags
	 * @return
	 */
	boolean applyChanges(PostProcessFlags flags);

	/**
	 * Mark all changes from the {@link ChangesList} as applied.
	 */
	void markAllAsApplied();

	/**
	 * Update the internally stored database version and mesh version in the mesh root vertex.
	 */
	void setCurrentVersionAndRev();
}
