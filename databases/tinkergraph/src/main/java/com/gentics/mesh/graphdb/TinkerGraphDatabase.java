package com.gentics.mesh.graphdb;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public class TinkerGraphDatabase implements Database {

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public FramedThreadedTransactionalGraph getFramedGraph(StorageOptions options) {
		ThreadedTransactionalGraphWrapper wrapper = new TinkerGraphThreadedTransactionalGraphWrapper(new TinkerTransactionalGraphMock());
		FramedThreadedTransactionalGraph fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
		return fg;
	}

}
