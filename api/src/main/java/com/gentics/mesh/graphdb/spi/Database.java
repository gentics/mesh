package com.gentics.mesh.graphdb.spi;

import java.io.IOException;

import com.gentics.mesh.etc.StorageOptions;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public interface Database {

	FramedThreadedTransactionalGraph getFramedGraph(StorageOptions options) throws IOException;

	void close();

	void reset();

	void clear();

}
