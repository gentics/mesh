package com.gentics.mesh.neo4j.tx;

import com.gentics.mesh.neo4j.Neo4jStorage;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.syncleus.ferma.tx.AbstractTx;
import com.syncleus.ferma.tx.Tx;
import com.syncleus.ferma.typeresolvers.TypeResolver;

public class Neo4jTx extends AbstractTx<FramedTransactionalGraph> {

	boolean isWrapped = false;
	
	public Neo4jTx(Neo4jStorage storage, TypeResolver typeResolver) {

		// Check if an active transaction already exists.
		Tx activeTx = Tx.getActive();
		if (activeTx != null) {
			isWrapped = true;
			init(activeTx.getGraph());
		} else {
			// TODO use tinkerpop instead
			//DelegatingFramedOrientGraph transaction = new DelegatingFramedOrientGraph(storage.getGraphDb().beginTx(), typeResolver);
			//init(transaction);
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
		} finally {
			if (!isWrapped) {
				// Restore the old graph that was previously swapped with the current graph
				getGraph().shutdown();
				Tx.setActive(null);
			}
		}
	}

}
