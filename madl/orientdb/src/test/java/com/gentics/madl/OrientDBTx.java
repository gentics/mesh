package com.gentics.madl;

import java.util.function.Function;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.madl.traversal.RawTraversalResultImpl;
import com.gentics.madl.tx.AbstractTx;
import com.gentics.madl.tx.Tx;
import com.gentics.mesh.madl.tp3.mock.Element;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.tp3.mock.GraphTraversalSource;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.syncleus.ferma.ext.orientdb.DelegatingFramedOrientGraph;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientDBTx extends AbstractTx<FramedTransactionalGraph> {

	boolean isWrapped = false;
	private final TypeResolver typeResolver;

	public OrientDBTx(OrientGraphFactory factory, TypeResolver typeResolver) {
		this.typeResolver = typeResolver;
		// Check if an active transaction already exists.
		Tx activeTx = Tx.get();
		if (activeTx != null) {
			isWrapped = true;
			init(activeTx.getGraph());
		} else {
			DelegatingFramedOrientGraph transaction = new DelegatingFramedOrientGraph(factory.getTx(), typeResolver);
			init(transaction);
		}
	}

	@Override
	public void close() {
		try {
			if (isSuccess()) {
				try {
					commit();
				} catch (Exception e) {
					rollback();
					throw e;
				}
			} else {
				rollback();
			}

		} catch (ONeedRetryException e) {
			throw e;
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
		return (T) new RawTraversalResultImpl(traverser.apply(rawTraverse()), typeResolver);
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
