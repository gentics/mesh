package com.gentics.mesh.dagger.tx;

import com.gentics.mesh.database.HibernateTx;

import dagger.Subcomponent;

/**
 * Transaction scoped binding.
 * 
 * @author plyhun
 *
 */
@TransactionScope
@Subcomponent(modules = TransactionModule.class)
public interface TransactionComponent {

	/**
	 * Get a current transaction.
	 * @return
	 */
	HibernateTx tx();

	/**
	 * Create a transaction provider.
	 * 
	 * @author plyhun
	 *
	 */
	@Subcomponent.Factory
	interface Factory {
		TransactionComponent create();
	}
}
