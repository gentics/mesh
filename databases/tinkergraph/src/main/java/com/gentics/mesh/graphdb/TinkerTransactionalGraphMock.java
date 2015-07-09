package com.gentics.mesh.graphdb;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class TinkerTransactionalGraphMock extends TinkerGraph implements TransactionalGraph {

	private static final long serialVersionUID = -8461785890451929357L;

	@Override
	public void stopTransaction(Conclusion conclusion) {
		// TODO Auto-generated method stub
	}

	@Override
	public void commit() {
		// TODO Auto-generated method stub

	}

	@Override
	public void rollback() {
		// TODO Auto-generated method stub

	}

}
