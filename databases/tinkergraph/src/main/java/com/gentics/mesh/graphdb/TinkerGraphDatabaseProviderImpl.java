package com.gentics.mesh.graphdb;

import com.gentics.mesh.etc.StorageOptions;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public class TinkerGraphDatabaseProviderImpl implements DatabaseServiceProvider {

	public TinkerGraphDatabaseProviderImpl() {

	}

	@Override
	public FramedThreadedTransactionalGraph getFramedGraph(StorageOptions options) {
		ThreadedTransactionalGraphWrapper wrapper = new TinkerGraphThreadedTransactionalGraphWrapper(new TinkerTransactionalGraphMock());
		FramedThreadedTransactionalGraph fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
		return fg;
	}

}
