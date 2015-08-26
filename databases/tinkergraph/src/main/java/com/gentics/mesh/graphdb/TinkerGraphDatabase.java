package com.gentics.mesh.graphdb;

import java.io.IOException;

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

	@Override
	public void backupGraph(String backupDirectory) {
		// Not supported
	}

	@Override
	public void restoreGraph(String backupFile) throws IOException {
		// Not supported
	}

	@Override
	public void exportGraph(String outputDirectory) {
		// Not supported
	}

	@Override
	public void importGraph(String importFile) throws IOException {
		// Not supported
	}
}
