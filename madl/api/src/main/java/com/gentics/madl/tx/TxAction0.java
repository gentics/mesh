
package com.gentics.madl.tx;

/**
 * Transaction operation.
 */
@FunctionalInterface
public interface TxAction0 {

	/**
	 * Run action in the transaction.
	 * 
	 * @throws Exception
	 */
	void handle() throws Exception;

}
