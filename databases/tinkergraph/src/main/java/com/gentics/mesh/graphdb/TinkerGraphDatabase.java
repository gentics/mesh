package com.gentics.mesh.graphdb;

import java.io.IOException;

import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.FramedTransactionalGraph;

public class TinkerGraphDatabase extends AbstractDatabase {

	private TinkerTransactionalGraphMock mockedGraph;

	@Override
	public void stop() {
	}

	@Override
	public FramedGraph startNonTransaction() {
		return new DelegatingFramedGraph<>(mockedGraph, true, false);
	}

	@Override
	public FramedTransactionalGraph startTransaction() {
		return new DelegatingFramedTransactionalGraph<>(mockedGraph, true, false);
	}

	@Override
	public void start() {
		mockedGraph = new TinkerTransactionalGraphMock();
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
