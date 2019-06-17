package com.gentics.mesh.neo4j.tx;

import java.util.function.Function;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.madl.tx.AbstractTx;
import com.gentics.madl.tx.Tx;
import com.gentics.mesh.madl.tp3.mock.Element;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.tp3.mock.GraphTraversalSource;
import com.gentics.mesh.neo4j.Neo4jStorage;
import com.syncleus.ferma.FramedTransactionalGraph;
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

	@Override
	public <T extends RawTraversalResult<?>> T traversal(Function<GraphTraversalSource, GraphTraversal<?, ?>> traverser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphTraversalSource rawTraverse() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T createVertex(Class<T> clazzOfR) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <E extends Element> E getElement(Object id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int txId() {
		// TODO Auto-generated method stub
		return 0;
	}

}
