package com.gentics.mesh.graphdb;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;

public class TitanDBDatabase extends AbstractDatabase {

	TitanGraph graph;

	@Override
	public void stop() {
		graph.shutdown();
	}

	@Override
	public NoTrx noTrx() {
		return new TitanDBNoTrx(new DelegatingFramedGraph<>(graph, true, false));
	}

	@Override
	public Trx trx() {
		return new TitanDBTrx(new DelegatingFramedTransactionalGraph<>(graph, true, false));
	}

	@Override
	public void start() {

		// Configuration configuration
		// graph = TitanFactory.open(configuration);
		// this.configuration = configuration;
		Configuration configuration = getBerkleyDBConf(options);
		graph = TitanFactory.open(configuration);

		// You may use getCassandraConf() or getInMemoryConf() to switch the backend graph db

		// Add some indices
		// graphDb.createKeyIndex("name", Vertex.class);
		// graphDb.createKeyIndex("ferma_type", Vertex.class);
		// graphDb.createKeyIndex("ferma_type", Edge.class);

	}

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
		conf.setProperty("storage.index.search.directory", settings.getDirectory());
		conf.setProperty("storage.index.search.client-only", "false");
		conf.setProperty("storage.index.search.local-mode", "true");

		return conf;
	}

	@Override
	public void reload(MeshElement element) {
		// Not supported
	}

	@Override
	public void exportGraph(String outputDirectory) {
		throw new NotImplementedException();
	}

	@Override
	public void importGraph(String importFile) {
		throw new NotImplementedException();
	}

	@Override
	public void backupGraph(String backupDirectory) {
		throw new NotImplementedException();
	}

	@Override
	public void restoreGraph(String backupFile) {
		throw new NotImplementedException();
	}
}
