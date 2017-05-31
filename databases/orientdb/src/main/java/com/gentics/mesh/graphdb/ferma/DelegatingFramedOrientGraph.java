package com.gentics.mesh.graphdb.ferma;

import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class DelegatingFramedOrientGraph<G extends OrientGraph> extends AbstractDelegatingFramedOrientGraph<G> {

	public DelegatingFramedOrientGraph(final G delegate, TypeResolver resolver) {
		super(delegate, resolver);
	}

}
