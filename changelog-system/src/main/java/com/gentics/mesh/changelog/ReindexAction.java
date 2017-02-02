package com.gentics.mesh.changelog;

/**
 * The reindex action is used to trigger a full reindex of the search provider index.
 */
@FunctionalInterface
public interface ReindexAction {

	/**
	 * Invokes the reindex action which should recreate all indices.
	 */
	public void invoke();
}
