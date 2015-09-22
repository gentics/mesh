package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class OrientDBTrx extends AbstractTrx {

	public OrientDBTrx(Database database) {
		FramedTransactionalGraph txGraph = database.startTransaction();
		((OrientGraph)((DelegatingFramedTransactionalGraph)txGraph).getBaseGraph()).getRawGraph().activateOnCurrentThread();
		init(database, txGraph);
	}

}
