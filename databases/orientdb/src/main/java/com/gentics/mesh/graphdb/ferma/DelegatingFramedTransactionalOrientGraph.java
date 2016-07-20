package com.gentics.mesh.graphdb.ferma;

import com.syncleus.ferma.WrapperFramedTransactionalGraph;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class DelegatingFramedTransactionalOrientGraph<G extends OrientGraph> extends AbstractDelegatingFramedOrientGraph<G>
		implements WrapperFramedTransactionalGraph<G> {

	public DelegatingFramedTransactionalOrientGraph(G delegate, TypeResolver resolver) {
		super(delegate, resolver);
	}

	@Override
	public void stopTransaction(Conclusion conclusion) {
		getBaseGraph().stopTransaction(conclusion);
	}

	@Override
	public void commit() {
		getBaseGraph().commit();
	}

	@Override
	public void rollback() {
		getBaseGraph().rollback();
	}

}
