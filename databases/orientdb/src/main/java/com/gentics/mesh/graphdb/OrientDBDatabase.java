package com.gentics.mesh.graphdb;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientDBDatabase implements Database {

	OrientGraphFactory factory = new OrientGraphFactory("memory:tinkerpop");//.setupPool(5, 100);

	private final OPartitionedDatabasePoolFactory poolFactory = new OPartitionedDatabasePoolFactory();

	public OrientDBDatabase() {

	}

	@Override
	public FramedThreadedTransactionalGraph getFramedGraph(StorageOptions options) {
		//OrientTransactionalGraph memoryGraph = new OrientGraph("memory:tinkerpop");

//		ODatabaseThreadLocalFactory customFactory = new MeshRecordFactory(poolFactory);
//		Orient.instance().registerThreadDatabaseFactory(customFactory);

		ThreadedTransactionalGraphWrapper wrapper = new OrientThreadedTransactionalGraphWrapper(factory);

		// Add some indices
		// memoryGraph.createKeyIndex("name", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Edge.class);

		FramedThreadedTransactionalGraph fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
		return fg;
	}

}
