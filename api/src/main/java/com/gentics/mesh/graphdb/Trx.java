package com.gentics.mesh.graphdb;

import com.syncleus.ferma.FramedGraph;

public interface Trx extends AutoCloseable {

	public static boolean debug = false;

	void success();

	void failure();

	FramedGraph getGraph();

	@Override
	void close();

}
