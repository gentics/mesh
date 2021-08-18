package com.gentics.mesh.graphdb.dagger;

import com.gentics.mesh.core.db.GraphDBTx;
import com.syncleus.ferma.ext.orientdb3.OrientDBTx;

import dagger.Binds;
import dagger.Module;

/**
 * Dagger bind module which binds OrientDB transaction specific types.
 */
@Module
public abstract class TransactionModule {

	@Binds
	abstract GraphDBTx tx(OrientDBTx tx);

}
