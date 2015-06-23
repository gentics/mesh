package com.gentics.mesh.graphdb;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Features;
import com.tinkerpop.blueprints.GraphQuery;
import com.tinkerpop.blueprints.ThreadedTransactionalGraph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

public abstract class ThreadedTransactionalGraphWrapper implements TransactionalGraph, ThreadedTransactionalGraph, ResettableGraph {

	TransactionalGraph graph;

	public ThreadedTransactionalGraphWrapper() {

	}

	@Override
	public Features getFeatures() {
		return graph.getFeatures();
	}

	@Override
	public Vertex addVertex(Object id) {
		return graph.addVertex(id);
	}

	@Override
	public Vertex getVertex(Object id) {
		return graph.getVertex(id);
	}

	@Override
	public void removeVertex(Vertex vertex) {
		graph.removeVertex(vertex);
	}

	@Override
	public Iterable<Vertex> getVertices() {
		return graph.getVertices();
	}

	@Override
	public Iterable<Vertex> getVertices(String key, Object value) {
		return graph.getVertices(key, value);
	}

	@Override
	public Edge addEdge(Object id, Vertex outVertex, Vertex inVertex, String label) {
		return graph.addEdge(id, outVertex, inVertex, label);
	}

	@Override
	public Edge getEdge(Object id) {
		return graph.getEdge(id);
	}

	@Override
	public void removeEdge(Edge edge) {
		graph.removeEdge(edge);
	}

	@Override
	public Iterable<Edge> getEdges() {
		return graph.getEdges();
	}

	@Override
	public Iterable<Edge> getEdges(String key, Object value) {
		return graph.getEdges(key, value);
	}

	@Override
	public GraphQuery query() {
		return graph.query();
	}

	@Override
	public void shutdown() {
		graph.shutdown();
	}

	public abstract TransactionalGraph newTransaction();

	@Override
	public void stopTransaction(Conclusion conclusion) {
		graph.stopTransaction(conclusion);
	}

	@Override
	public void commit() {
		graph.commit();
	}

	@Override
	public void rollback() {
		graph.rollback();
	}

	@Override
	public TransactionalGraph getGraph() {
		return graph;
	}

	@Override
	public void setGraph(TransactionalGraph graph) {
		this.graph = graph;
	}

}
