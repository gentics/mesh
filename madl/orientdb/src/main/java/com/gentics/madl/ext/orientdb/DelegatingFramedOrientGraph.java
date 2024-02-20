package com.gentics.madl.ext.orientdb;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;

import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.DefaultClassInitializer;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.typeresolvers.TypeResolver;

public class DelegatingFramedOrientGraph extends DelegatingFramedGraph<OrientGraph> {

	public DelegatingFramedOrientGraph(OrientGraph delegate, TypeResolver defaultResolver) {
		super(delegate, defaultResolver);
	}

	@Override
	public <T> T addFramedVertex(final ClassInitializer<T> initializer, Object... id) {
		return frameNewElement(this.getBaseGraph().addVertex(id), initializer);
	}

	@Override
	public <T> T addFramedEdge(VertexFrame source, VertexFrame destination, String label, ClassInitializer<T> initializer, Object... id) {
		return frameNewElement(source.getElement().addEdge(label, destination.getElement(), id), initializer);
	}

	@Override
	public <T> T addFramedVertex(final Class<T> kind) {
		return this.addFramedVertex(new DefaultClassInitializer<>(kind), "class:" + kind.getSimpleName());
	}

	@Override
	public <T> T addFramedEdge(VertexFrame source, VertexFrame destination, String label, Class<T> kind) {
		return super.addFramedEdge(source, destination, label, kind);
	}
}
