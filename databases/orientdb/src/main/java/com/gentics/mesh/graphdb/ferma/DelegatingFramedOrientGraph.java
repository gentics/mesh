package com.gentics.mesh.graphdb.ferma;

import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class DelegatingFramedOrientGraph<G extends OrientGraphNoTx> extends AbstractDelegatingFramedOrientGraph<G> {

	public DelegatingFramedOrientGraph(final G delegate, final boolean typeResolution, final boolean annotationsSupported) {
		super(delegate, typeResolution, annotationsSupported);
	}


}
