package com.gentics.madl.ext.arcadedb;

import java.util.HashMap;
import java.util.Map;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Element;

import com.arcadedb.gremlin.ArcadeGraph;
import com.gentics.madl.graph.DelegatingFramedMadlGraph;
import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.DefaultClassInitializer;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.typeresolvers.TypeResolver;

public class DelegatingFramedArcadeGraph extends DelegatingFramedGraph<ArcadeGraph> implements DelegatingFramedMadlGraph<ArcadeGraph> {

	private final Map<String, Object> attributes = new HashMap<>();

	public DelegatingFramedArcadeGraph(ArcadeGraph delegate, TypeResolver defaultResolver) {
		super(delegate, defaultResolver);
	}

	@Override
	public <T> T addFramedVertex(final ClassInitializer<T> initializer, Object... id) {
		return frameNewElement(this.getBaseGraph().addVertex(id), initializer);
	}

	@Override
	public <T> T addFramedEdge(VertexFrame source, VertexFrame destination, String label,
			ClassInitializer<T> initializer, Object... id) {
		return frameNewElement(source.getElement().addEdge(label, destination.getElement(), id), initializer);
	}

	@Override
	public <T> T addFramedVertex(final Class<T> kind) {
		return this.addFramedVertex(new DefaultClassInitializer<>(kind), org.apache.tinkerpop.gremlin.structure.T.label, kind.getSimpleName());
	}

	@Override
	public <T> T addFramedEdge(VertexFrame source, VertexFrame destination, String label, Class<T> kind) {
		return super.addFramedEdge(source, destination, label, kind);
	}

	@Override
	public <T> T getAttribute(String key) {
		return (T) attributes.get(key);
	}

	@Override
	public void setAttribute(String key, Object value) {
		attributes.put(key, value);
	}

	public static <T extends Element> GraphTraversal<?,T> getElements(GraphTraversal<?, T> traversal, final String label, final String[] iKey, Object[] iValue) {
		for (int i = 0; i < iKey.length; i++) {
			traversal = traversal.has(iKey[i], iValue[i]);
		}
		return traversal;
	}
}
