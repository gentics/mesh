package com.gentics.mesh.core.migration;

/**
 * Exception that is thrown when a migration has been aborted
 */
public class MigrationAbortedException extends Exception {
	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = 2877461998431596437L;

	/**
	 * Create instance
	 * @param reason reason for the abort
	 */
	public MigrationAbortedException(String reason) {
		super(String.format("The migration job has been aborted. Reason: %s", reason));
	}
}
