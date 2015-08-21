package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientDBDatabase extends AbstractDatabase {

	private static final Logger log = LoggerFactory.getLogger(OrientDBDatabase.class);

	private OrientGraphFactory factory;
	private OrientThreadedTransactionalGraphWrapper wrapper;

	@Override
	public void stop() {
		factory.close();
		Orient.instance().shutdown();
		Trx.setLocalGraph(null);
	}

	@Override
	public void start() {
		Orient.instance().startup();
		//OGlobalConfiguration.CACHE_LOCAL_ENABLED.setValue(false);
		factory = new OrientGraphFactory("memory:tinkerpop");
		// Add some indices
		// memoryGraph.createKeyIndex("name", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Edge.class);

		// factory = new OrientGraphFactory("plocal:" + options.getDirectory());// .setupPool(5, 100);
//		wrapper.setFactory(factory);
		wrapper = new OrientThreadedTransactionalGraphWrapper(factory);
		fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
	}

	@Override
	public void reload(MeshElement element) {
		((OrientVertex) element.getElement()).reload();
	}

}
