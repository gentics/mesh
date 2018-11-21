package com.gentics.diktyo.tx;

public interface Tx extends BaseTransaction {

	static ThreadLocal<Tx> threadLocalTx = new ThreadLocal<>();

	static Tx get() {
		return Tx.threadLocalTx.get();
	}

	static void set(Tx tx) {
		Tx.threadLocalTx.set(tx);
	}

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

}
