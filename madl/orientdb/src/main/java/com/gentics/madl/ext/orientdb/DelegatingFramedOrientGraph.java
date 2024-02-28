package com.gentics.madl.ext.orientdb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal.Symbols;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;
import org.apache.tinkerpop.gremlin.structure.Element;

import com.gentics.madl.graph.DelegatingFramedMadlGraph;
import com.orientechnologies.orient.core.index.OIndexInternal;
import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.DefaultClassInitializer;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.typeresolvers.TypeResolver;

public class DelegatingFramedOrientGraph extends DelegatingFramedGraph<OrientGraph> implements DelegatingFramedMadlGraph<OrientGraph> {

	private final Map<String, Object> attributes = new HashMap<>();

	public DelegatingFramedOrientGraph(OrientGraph delegate, TypeResolver defaultResolver) {
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
		return this.addFramedVertex(new DefaultClassInitializer<>(kind));
	}

	@Override
	public <T> T addFramedEdge(VertexFrame source, VertexFrame destination, String label, Class<T> kind) {
		return super.addFramedEdge(source, destination, label, kind);
	}

	@Override
	public <T> Optional<Iterator<? extends T>> maybeGetIndexedFramedElements(String index, Object id, Class<T> kind) {
		return Optional.of(((OIndexInternal) getBaseGraph().getRawDatabase().getMetadata().getIndexManager().getIndex(index)).getRids(id)).map(rids -> (Iterator) getBaseGraph().edges(rids.collect(Collectors.toList()).toArray()));
	}

	@Override
	public <T> T getAttribute(String key) {
		return (T) attributes.get(key);
	}

	@Override
	public void setAttribute(String key, Object value) {
		attributes.put(key, value);
	}

	public static <T extends Element> GraphTraversal<?,T> getElements(OrientGraph graph, final String label, final String[] iKey, Object[] iValue) {
		DefaultGraphTraversal<?,T> traversal = new DefaultGraphTraversal<>(graph);
		for (int i = 0; i < iKey.length; i++) {
			traversal.asAdmin().getBytecode().addStep(Symbols.has, iKey[i], iValue[i]);
	        TraversalHelper.addHasContainer(traversal.asAdmin(), new HasContainer(iKey[i], P.eq(iValue[i])));
		}
		return traversal;
	}
}
