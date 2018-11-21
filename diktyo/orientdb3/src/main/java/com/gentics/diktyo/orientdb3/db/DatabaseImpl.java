package com.gentics.diktyo.orientdb3.db;

import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.diktyo.db.AbstractDatabase;
import com.gentics.diktyo.db.DatabaseType;
import com.gentics.diktyo.index.IndexManager;
import com.gentics.diktyo.orientdb3.tx.TxImpl;
import com.gentics.diktyo.orientdb3.wrapper.factory.WrapperFactory;
import com.gentics.diktyo.tx.Tx;
import com.gentics.diktyo.tx.TxAction;
import com.gentics.diktyo.wrapper.traversal.WrappedTraversal;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;

public class DatabaseImpl extends AbstractDatabase {

	private final IndexManager indexManager;

	private final Provider<TxImpl> txProvider;

	private OrientGraphFactory graph;

	@Inject
	public DatabaseImpl(IndexManager indexManager, Provider<TxImpl> txProvider) {
		this.indexManager = indexManager;
		this.txProvider = txProvider;
	}

	@Override
	public void open(String name, DatabaseType type) {
		OrientDB odb = new OrientDB("embedded:.", OrientDBConfig.defaultConfig());
		graph = new OrientGraphFactory(odb, name, ODatabaseType.MEMORY, "admin", "admin");
	}

	@Override
	public void close() {
		graph.close();
		graph = null;
	}

	@Override
	public boolean isOpen() {
		return graph != null;
	}

	@Override
	public IndexManager index() {
		return indexManager;
	}

	@Override
	public Tx tx() {
		TxImpl tx = txProvider.get();
		// Add graph reference
		return tx;
	}

	@Override
	public <T> T tx(TxAction<T> txHandler) {
		return null;
	}

	@Override
	public <T> T createVertex(Class<T> clazzOfR) {
		Vertex vertex = graph.getNoTx().addVertex();
		return WrapperFactory.frameVertex(vertex, clazzOfR);
	}

	@Override
	public <T extends WrappedTraversal<?>> T traverse(Function<GraphTraversalSource, GraphTraversal<?, ?>> traverser) {
		// TODO Auto-generated method stub
		return null;
	}

}
