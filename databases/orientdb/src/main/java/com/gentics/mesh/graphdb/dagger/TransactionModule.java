package com.gentics.mesh.graphdb.dagger;

import com.gentics.mesh.core.db.Tx;
import com.syncleus.ferma.ext.orientdb3.OrientDBTx;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class TransactionModule {

	@Binds
	abstract Tx tx(OrientDBTx tx);

}
