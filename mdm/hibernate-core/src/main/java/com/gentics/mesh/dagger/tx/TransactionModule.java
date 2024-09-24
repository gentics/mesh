package com.gentics.mesh.dagger.tx;

import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.database.HibTxData;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.HibernateTxImpl;

import dagger.Binds;
import dagger.Module;

/**
 * Transaction data Dagger 2 bindings.
 * 
 * @author plyhun
 *
 */
@Module
public abstract class TransactionModule {
	@Binds
	abstract HibernateTx tx(HibernateTxImpl tx);

	@Binds
	abstract TxData txData(HibTxData tx);
}
