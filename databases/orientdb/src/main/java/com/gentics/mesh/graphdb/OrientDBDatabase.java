package com.gentics.mesh.graphdb;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.orientechnologies.orient.core.Orient;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientDBDatabase implements Database {

	private static final Logger log = LoggerFactory.getLogger(OrientDBDatabase.class);

	private OrientGraphFactory factory;
	private OrientThreadedTransactionalGraphWrapper wrapper;
	private StorageOptions options;
	private FramedThreadedTransactionalGraph fg;

	@Override
	public void close() {
		factory.close();
	}

	@Override
	public void reset() {
		if (log.isDebugEnabled()) {
			log.debug("Resetting orientdb");
		}
		factory.close();
		Orient.instance().shutdown();
		try {
			FileUtils.deleteDirectory(new File(options.getDirectory()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Orient.instance().startup();
		// factory = new OrientGraphFactory("memory:tinkerpop");
		factory = new OrientGraphFactory("plocal:" + options.getDirectory());// .setupPool(5, 100);
		wrapper.setFactory(factory);
		wrapper.setGraph(factory.getTx());
	}

	@Override
	public void clear() {
		if (log.isDebugEnabled()) {
			log.debug("Clearing orientdb {" + factory.hashCode() + "}");
		}
		fg.e().removeAll();
		fg.v().removeAll();
		if (log.isDebugEnabled()) {
			log.debug("Cleared orientdb {" + factory.hashCode() + "}");
		}
	}

	@Override
	public void init(StorageOptions options) {
		this.options = options;
		factory = new OrientGraphFactory("plocal:" + options.getDirectory());// .setupPool(5, 100);
		wrapper = new OrientThreadedTransactionalGraphWrapper(factory);
		fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
	}

	@Override
	public FramedThreadedTransactionalGraph getFramedGraph() {

		// factory = new OrientGraphFactory("memory:tinkerpop");

		// Add some indices
		// memoryGraph.createKeyIndex("name", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Edge.class);

		return fg;
	}

}
