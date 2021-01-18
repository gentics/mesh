package com.gentics.madl.tx;

/**
 * Transaction operation.
 * 
 * @param <T>
 *            Return type of the action
 */
@FunctionalInterface
public interface TxAction<T> {

	/**
	 * Run action in the transaction.
	 * 
	 * @param tx
	 *            Transaction
	 * @return Return object
	 * @throws Exception
	 */
	T handle(Tx tx) throws Exception;

}
