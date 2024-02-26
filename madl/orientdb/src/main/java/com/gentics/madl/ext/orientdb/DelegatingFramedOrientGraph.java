package com.gentics.madl.ext.orientdb;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal.Symbols;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;
import org.apache.tinkerpop.gremlin.structure.Element;

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
	public <T> T addFramedEdge(VertexFrame source, VertexFrame destination, String label,
			ClassInitializer<T> initializer, Object... id) {
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

	public static <T extends Element> GraphTraversal<?,T> getElements(OrientGraph graph, final String label, final String[] iKey, Object[] iValue) {
		DefaultGraphTraversal<?,T> traversal = new DefaultGraphTraversal<>(graph);
		for (int i = 0; i < iKey.length; i++) {
			traversal.asAdmin().getBytecode().addStep(Symbols.has, iKey[i], iValue[i]);
	        TraversalHelper.addHasContainer(traversal.asAdmin(), new HasContainer(iKey[i], P.eq(iValue[i])));
		}
		return traversal;
	}
}
