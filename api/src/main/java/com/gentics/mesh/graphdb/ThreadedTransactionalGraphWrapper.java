package com.gentics.mesh.graphdb;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Features;
import com.tinkerpop.blueprints.GraphQuery;
import com.tinkerpop.blueprints.ThreadedTransactionalGraph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

public abstract class ThreadedTransactionalGraphWrapper implements TransactionalGraph, ThreadedTransactionalGraph {

	public abstract TransactionalGraph getGraph();

	@Override
	public Features getFeatures() {
		return getGraph().getFeatures();
	}

	@Override
	public Vertex addVertex(Object id) {
		return getGraph().addVertex(id);
	}

	@Override
	public Vertex getVertex(Object id) {
		return getGraph().getVertex(id);
	}

	@Override
	public void removeVertex(Vertex vertex) {
		getGraph().removeVertex(vertex);
	}

	@Override
	public Iterable<Vertex> getVertices() {
		return getGraph().getVertices();
	}

	@Override
	public Iterable<Vertex> getVertices(String key, Object value) {
		return getGraph().getVertices(key, value);
	}

	@Override
	public Edge addEdge(Object id, Vertex outVertex, Vertex inVertex, String label) {
		return getGraph().addEdge(id, outVertex, inVertex, label);
	}

	@Override
	public Edge getEdge(Object id) {
		return getGraph().getEdge(id);
	}

	@Override
	public void removeEdge(Edge edge) {
		getGraph().removeEdge(edge);
	}

	@Override
	public Iterable<Edge> getEdges() {
		return getGraph().getEdges();
	}

	@Override
	public Iterable<Edge> getEdges(String key, Object value) {
		return getGraph().getEdges(key, value);
	}

	@Override
	public GraphQuery query() {
		return getGraph().query();
	}

	@Override
	public void shutdown() {
		getGraph().shutdown();
	}

	public abstract TransactionalGraph newTransaction();

	@SuppressWarnings("deprecation")
	@Override
	public void stopTransaction(Conclusion conclusion) {
		getGraph().stopTransaction(conclusion);
	}

	@Override
	public void commit() {
		getGraph().commit();
	}

	@Override
	public void rollback() {
		getGraph().rollback();
	}

}
