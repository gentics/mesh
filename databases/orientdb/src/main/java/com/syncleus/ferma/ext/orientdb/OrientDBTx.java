package com.syncleus.ferma.ext.orientdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.tx.OrientStorage;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.syncleus.ferma.tx.AbstractTx;
import com.syncleus.ferma.tx.Tx;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientDBTx extends AbstractTx<FramedTransactionalGraph> {

	boolean isWrapped = false;

	public OrientDBTx(OrientGraphFactory factory, TypeResolver typeResolver) {

		// Check if an active transaction already exists.
		Tx activeTx = Tx.getActive();
		if (activeTx != null) {
			isWrapped = true;
			init(activeTx.getGraph());
		} else {
			DelegatingFramedOrientGraph transaction = new DelegatingFramedOrientGraph(factory.getTx(), typeResolver);
			init(transaction);
		}
	}

	public OrientDBTx(OrientStorage provider, TypeResolver typeResolver) {

		// Check if an active transaction already exists.
		Tx activeTx = Tx.getActive();
		if (activeTx != null) {
			isWrapped = true;
			init(activeTx.getGraph());
		} else {
			DelegatingFramedOrientGraph transaction = new DelegatingFramedOrientGraph((OrientGraph)provider.rawTx(), typeResolver);
			init(transaction);
		}
	}

	@Override
	public void close() {
		try {
			if (isSuccess()) {
				commit();
			} else {
				rollback();
			}
		} catch (OConcurrentModificationException e) {
			throw e;
		} finally {
			if (!isWrapped) {
				// Restore the old graph that was previously swapped with the current graph
				getGraph().shutdown();
				Tx.setActive(null);
			}
		}
	}
}
