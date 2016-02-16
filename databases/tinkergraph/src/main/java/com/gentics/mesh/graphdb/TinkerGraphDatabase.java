package com.gentics.mesh.graphdb;

import java.io.IOException;
import java.util.Iterator;

import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.gentics.mesh.graphdb.spi.TrxHandler;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

public class TinkerGraphDatabase extends AbstractDatabase {

	private TinkerTransactionalGraphMock mockedGraph;

	@Override
	public void stop() {
	}

	@Override
	public NoTrx noTrx() {
		return new TinkergraphNoTrx(new DelegatingFramedGraph<>(mockedGraph, true, false));
	}

	@Override
	public Trx trx() {
		return new TinkergraphTrx(new DelegatingFramedTransactionalGraph<>(mockedGraph, true, false));
	}

	@Override
	public void start() {
		mockedGraph = new TinkerTransactionalGraphMock();
	}

	@Override
	public void reload(MeshElement element) {
		// Not supported
	}

	@Override
	public void backupGraph(String backupDirectory) {
		// Not supported
	}

	@Override
	public void restoreGraph(String backupFile) throws IOException {
		// Not supported
	}

	@Override
	public void exportGraph(String outputDirectory) {
		// Not supported
	}

	@Override
	public void importGraph(String importFile) throws IOException {
		// Not supported
	}

	@Override
	public <T> T trx(TrxHandler<T> txHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addEdgeIndex(String label, String... extraFields) {
		// No supported
	}

	@Override
	public void addVertexIndex(Class<?> clazzOfVertices, String... fields) {
		// No supported
	}

	@Override
	public void addEdgeIndexSource(String label) {
		// No supported
	}

	@Override
	public Object createComposedIndexKey(Object... keys) {
		return null;
	}

	@Override
	public void addEdgeType(String label, String... stringPropertyKeys) {
		// TODO Auto-generated method stub
	}

	@Override
	public void addVertexType(Class<?> clazzOfVertex) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public Iterator<Vertex> getVertices(Class<?> classOfVertex, String[] fieldNames, Object[] fieldValues) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void setVertexType(Element element, Class<?> classOfVertex) {
		// TODO Auto-generated method stub
		
	}
}
