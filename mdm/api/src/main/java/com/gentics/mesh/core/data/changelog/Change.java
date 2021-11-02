package com.gentics.mesh.core.data.changelog;

/**
 * Domain model for a database changelog entry.
 */
public interface Change {

	/**
	 * Return the uuid of the change.
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Return the name of the change.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Apply the change to the graph which need a transaction.
	 */
	void apply();

	/**
	 * Apply changes which need to be executed outside of a transaction.
	 */
	void applyNoTx();

	/**
	 * Return the description of the change.
	 * 
	 * @return
	 */
	String getDescription();

	/**
	 * Return a flag which informs the changelog system whether the change requires a reindex.
	 * 
	 * @return
	 */
	default boolean requiresReindex() {
		return false;
	}
}
