package com.gentics.mesh.graphdb.spi;

import com.gentics.mesh.etc.StorageOptions;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public interface Database {

	FramedThreadedTransactionalGraph getFramedGraph();

	void close();

	void reset();

	void clear();

	void init(StorageOptions options);

}
