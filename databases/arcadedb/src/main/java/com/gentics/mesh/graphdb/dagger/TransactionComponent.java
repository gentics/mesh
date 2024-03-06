package com.gentics.mesh.graphdb.dagger;

import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.graphdb.tx.OrientStorage;
import com.syncleus.ferma.typeresolvers.TypeResolver;

import dagger.BindsInstance;
import dagger.Subcomponent;

/**
 * The transaction component is used to create a dagger subcomponent which has a dedicated dependency graph for every created transaction.
 */
@TransactionScope
@Subcomponent(modules = TransactionModule.class)
public interface TransactionComponent {

	/**
	 * Return the transaction itself.
	 * 
	 * @return
	 */
	GraphDBTx tx();

	/**
	 * Factory for new {@link TransactionComponent} instances.
	 */
	@Subcomponent.Factory
	interface Factory {
		TransactionComponent create(@BindsInstance OrientStorage txProvider, @BindsInstance TypeResolver resolver);
	}
}
