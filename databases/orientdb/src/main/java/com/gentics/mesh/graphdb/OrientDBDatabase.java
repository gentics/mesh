package com.gentics.mesh.graphdb;

import java.io.File;

import org.apache.tools.ant.util.FileUtils;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientDBDatabase implements Database {

	private OrientGraphFactory factory;

	public OrientDBDatabase() {

	}

	@Override
	public void close() {
		factory.close();
	}

	@Override
	public void reset() {
		//		factory.drop();
		//factory.close();
		//factory = new OrientGraphFactory("memory:tinkerpop");

	}

	@Override
	public void clear() {
//		for (Edge edge : factory.getNoTx().getEdges()) {
//			edge.remove();
//		}
		for (Vertex vertex : factory.getNoTx().getVertices()) {
			vertex.remove();
		}
	}

	@Override
	public FramedThreadedTransactionalGraph getFramedGraph(StorageOptions options) {

		FileUtils.delete(new File(options.getDirectory()));
//		factory = new OrientGraphFactory("memory:tinkerpop");
		factory = new OrientGraphFactory("plocal:"+ options.getDirectory());//.setupPool(5, 100);
		ThreadedTransactionalGraphWrapper wrapper = new OrientThreadedTransactionalGraphWrapper(factory);

		// Add some indices
		// memoryGraph.createKeyIndex("name", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Edge.class);

		FramedThreadedTransactionalGraph fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
		return fg;
	}

}
