package com.gentics.mesh.graphdb.dagger;

import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.graphdb.ferma.ArcadeDBTx;

import dagger.Binds;
import dagger.Module;

/**
 * Dagger bind module which binds ArcadeDB transaction specific types.
 */
@Module
public abstract class TransactionModule {

	@Binds
	abstract GraphDBTx tx(ArcadeDBTx tx);

}
