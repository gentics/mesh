package com.gentics.mesh.changelog;

import java.util.List;

import com.gentics.mesh.changelog.changes.ChangesList;

public interface ChangelogSystem {
	/**
	 * Apply all listed changes.
	 *
	 * @param reindexAction
	 * @param list
	 * @return Flag which indicates whether all changes were applied successfully
	 */
	boolean applyChanges(ReindexAction reindexAction, List<Change> list);

	/**
	 * Mark all changelog entries as applied. This is useful if you resolved issues manually or if you want to create a fresh mesh database dump.
	 */
	void markAllAsApplied(List<Change> list);

	/**
	 * Apply all changes from the {@link ChangesList}.
	 *
	 * @param reindexAction
	 * @return
	 */
	boolean applyChanges(ReindexAction reindexAction);

	/**
	 * Mark all changes from the {@link ChangesList} as applied.
	 */
	void markAllAsApplied();

	/**
	 * Update the internally stored database version and mesh version in the mesh root vertex.
	 */
	void setCurrentVersionAndRev();
}
