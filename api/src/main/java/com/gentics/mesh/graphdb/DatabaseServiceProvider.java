package com.gentics.mesh.graphdb;

import java.io.IOException;

import com.gentics.mesh.etc.StorageOptions;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public interface DatabaseServiceProvider {

	FramedThreadedTransactionalGraph getFramedGraph(StorageOptions options) throws IOException;

}
