package com.gentics.mesh.core.db;

public interface BaseTransaction extends AutoCloseable {

	/**
	 * Commit the transaction.
	 */
	void commit();

	/**
	 * Rollback the transaction.
	 */
	void rollback();

	/**
	 * Mark the transaction as succeeded. The autoclosable will invoke a commit when completing.
	 */
	void success();

	/**
	 * Mark the transaction as failed. The autoclosable will invoke a rollback when completing.
	 */
	void failure();

	/**
	 * Invoke rollback or commit when closing the autoclosable. By default a rollback will be invoked.
	 */
	@Override
	void close();

	/**
	 * Return the id of the transaction.
	 * 
	 * @return
	 */
	int txId();

}
