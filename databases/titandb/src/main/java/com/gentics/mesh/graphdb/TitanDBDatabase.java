package com.gentics.mesh.graphdb;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public class TitanDBDatabase implements Database {

	private Configuration getBerkleyDBConf(StorageOptions settings) {
		Configuration conf = new BaseConfiguration();
		conf.setProperty("storage.backend", "berkeleyje");
		conf.setProperty("storage.directory", settings.getDirectory());
		return conf;
	}

	private Configuration getInMemoryConf(StorageOptions settings) {
		Configuration conf = new BaseConfiguration();
		conf.setProperty("storage.backend", "inmemory");
		return conf;
	}

	private Configuration getCassandraConf(StorageOptions settings) {
		Configuration conf = new BaseConfiguration();
		conf.setProperty("storage.backend", "embeddedcassandra");
		conf.setProperty("storage.cassandra-config-dir", "config/cassandra.yaml");
		conf.setProperty("cache.db-cache", "true");
		conf.setProperty("cache.tx-cache-size", "100000");
		conf.setProperty("storage.index.search.backend", "elasticsearch");
		conf.setProperty("storage.index.search.directory", "/tmp/searchindex");
		conf.setProperty("storage.index.search.client-only", "false");
		conf.setProperty("storage.index.search.local-mode", "true");

		return conf;
	}

	@Override
	public FramedThreadedTransactionalGraph getFramedGraph(StorageOptions settings) {
		// You may use getCassandraConf() or getInMemoryConf() to switch the backend graph db

		// Add some indices
		// graphDb.createKeyIndex("name", Vertex.class);
		// graphDb.createKeyIndex("ferma_type", Vertex.class);
		// graphDb.createKeyIndex("ferma_type", Edge.class);
		ThreadedTransactionalGraphWrapper wrapper = new TitanDBThreadedTransactionalGraphWrapper(getBerkleyDBConf(settings));
		FramedThreadedTransactionalGraph fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
		return fg;
	}

}
