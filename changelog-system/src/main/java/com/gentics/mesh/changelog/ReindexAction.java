package com.gentics.mesh.changelog;

@FunctionalInterface
public interface ReindexAction {

	/**
	 * Invokes the reindex action which should recreate all indices.
	 */
	public void invoke();
}
