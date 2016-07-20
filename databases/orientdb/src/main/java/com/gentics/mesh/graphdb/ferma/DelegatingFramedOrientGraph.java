package com.gentics.mesh.graphdb.ferma;

import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class DelegatingFramedOrientGraph<G extends OrientGraphNoTx> extends AbstractDelegatingFramedOrientGraph<G> {

	public DelegatingFramedOrientGraph(final G delegate, TypeResolver resolver) {
		super(delegate, resolver);
	}

}
