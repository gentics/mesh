package com.gentics.mesh.graphdb;

import io.vertx.core.json.JsonObject;

import com.gentics.mesh.graphdb.DatabaseServiceProvider;
import com.gentics.mesh.graphdb.ThreadedTransactionalGraphWrapper;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public class TinkerGraphDatabaseProviderImpl implements DatabaseServiceProvider {

	public TinkerGraphDatabaseProviderImpl() {

	}

	@Override
	public FramedThreadedTransactionalGraph getFramedGraph(JsonObject settings) {
		ThreadedTransactionalGraphWrapper wrapper = new TinkerGraphThreadedTransactionalGraphWrapper(new TinkerTransactionalGraphMock());
		FramedThreadedTransactionalGraph fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
		return fg;
	}

}
