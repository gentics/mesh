package com.gentics.mesh.graphdb.dagger;

import com.gentics.mesh.core.db.Tx;
import com.syncleus.ferma.ext.orientdb3.OrientDBTx;

import dagger.Binds;
import dagger.Module;

/**
 * Dagger bind module which binds OrientDB transaction specific types.
 */
@Module
public abstract class TransactionModule {

	@Binds
	abstract Tx tx(OrientDBTx tx);

}
