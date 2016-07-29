package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.ferma.DelegatingFramedTransactionalOrientGraph;
import com.gentics.mesh.graphdb.spi.Database;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

/**
 * OrientDB specific implementation of an tx autoclosable.
 */
public class OrientDBTx extends AbstractTx {

	public OrientDBTx(OrientGraphFactory factory, TypeResolver resolver) {
		OrientGraph tx = factory.getTx();
		FramedTransactionalGraph transaction = new DelegatingFramedTransactionalOrientGraph(tx, resolver);
		init(transaction);
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
			// Restore the old graph that was previously swapped with the current graph
			getGraph().shutdown();
			Database.setThreadLocalGraph(getOldGraph());
		}
	}
}
