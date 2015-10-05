package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientDBTrx extends AbstractTrx {

	public OrientDBTrx(OrientGraphFactory factory) {
		FramedTransactionalGraph transaction = new DelegatingFramedTransactionalGraph<>(factory.getTx(), true, false);
		// ((OrientGraph)((DelegatingFramedTransactionalGraph)txGraph).getBaseGraph()).getRawGraph().activateOnCurrentThread();
		init(transaction);
	}

}
