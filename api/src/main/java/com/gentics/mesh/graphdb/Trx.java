package com.gentics.mesh.graphdb;

import com.syncleus.ferma.FramedGraph;

public interface Trx extends AutoCloseable {

	void success();

	void failure();

	FramedGraph getGraph();

	@Override
	void close();

}
