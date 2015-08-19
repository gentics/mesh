package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;

public class TinkerGraphDatabase extends AbstractDatabase {

	ThreadedTransactionalGraphWrapper wrapper;

	@Override
	public void stop() {
	}

	@Override
	public void start() {
		wrapper = new TinkerGraphThreadedTransactionalGraphWrapper(new TinkerTransactionalGraphMock());
		fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
	}

	@Override
	public void reload(MeshElement element) {
		// Not supported
	}

}
