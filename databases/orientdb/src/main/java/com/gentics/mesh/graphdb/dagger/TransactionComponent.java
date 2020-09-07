package com.gentics.mesh.graphdb.dagger;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.graphdb.tx.OrientStorage;
import com.syncleus.ferma.typeresolvers.TypeResolver;

import dagger.BindsInstance;
import dagger.Subcomponent;

@TransactionScope
@Subcomponent(modules = TransactionModule.class)
public interface TransactionComponent {

	Tx tx();

	@Subcomponent.Factory
	interface Factory {
		TransactionComponent create(@BindsInstance OrientStorage txProvider, @BindsInstance TypeResolver resolver);
	}
}
