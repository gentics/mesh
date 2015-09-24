package com.gentics.mesh.graphdb;

import com.syncleus.ferma.FramedTransactionalGraph;

public class OrientDBTrx extends AbstractTrx {

	public OrientDBTrx(FramedTransactionalGraph transaction) {
		//((OrientGraph)((DelegatingFramedTransactionalGraph)txGraph).getBaseGraph()).getRawGraph().activateOnCurrentThread();
		init(transaction);
	}

}
