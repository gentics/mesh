package com.gentics.mesh.graphdb;

import com.syncleus.ferma.FramedGraph;

public interface NoTrx extends AutoCloseable {

	void close();

	FramedGraph getGraph();
}
