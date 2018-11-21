package com.gentics.diktyo.tx;

import com.gentics.diktyo.type.TypeManager;

/**
 * Transaction which will automatically commit after each operation. This kind of transaction should only be used when dealing with indices, types. Some vendor
 * implementations may behave unpredictable if errors occur during transaction opertation.
 */
public interface NoTx extends BaseTransaction {

	/**
	 * Return the type manager.
	 * 
	 * @return
	 */
	TypeManager type();
}
