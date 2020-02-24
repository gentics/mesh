package com.gentics.madl.ext.orientdb;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.ElementTypeClassCache;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;

public class OrientDBTypeResolver implements TypeResolver {

	private final ElementTypeClassCache elementTypCache;

	public OrientDBTypeResolver(String... packagePaths) {
		this.elementTypCache = new ElementTypeClassCache(packagePaths);
	}

	@Override
	public <T> Class<? extends T> resolve(Element element, Class<T> kind) {
		if (element instanceof WrappedElement) {
			element = ((WrappedElement) element).getBaseElement();
		}
		if (element instanceof OrientVertex) {
			OrientVertex orientVertex = (OrientVertex) element;
			String name = orientVertex.getType().getName();
			return resolve(name, kind);
		}
		if (element instanceof OrientEdge) {
			OrientEdge orientEdge = (OrientEdge) element;
			String name = orientEdge.getType().getSuperClass().getName();
			return resolve(name, kind);
		}
		return null;
	}

	private <T> Class<? extends T> resolve(String type, Class<T> kind) {
		final Class<T> nodeKind = (Class<T>) this.elementTypCache.forName(type);
		if (kind.isAssignableFrom(nodeKind) || kind.equals(VertexFrame.class) || kind.equals(EdgeFrame.class)
			|| kind.equals(AbstractVertexFrame.class) || kind.equals(AbstractEdgeFrame.class) || kind.equals(Object.class)) {
			return nodeKind;
		} else {
			return kind;
		}
	}

	@Override
	public Class<?> resolve(Element element) {
		if (element instanceof OrientVertex) {
			OrientVertex orientVertex = (OrientVertex) element;
			String name = orientVertex.getType().getName();
			return this.elementTypCache.forName(name);
		}
		if (element instanceof OrientEdge) {
			OrientEdge orientEdge = (OrientEdge) element;
			String name = orientEdge.getType().getName();
			return this.elementTypCache.forName(name);
		}
		return null;
	}

	@Override
	public void init(Element element, Class<?> kind) {
		// NOP
	}

	@Override
	public void deinit(Element element) {
		throw new NotImplementedException("DeInit is not yet supported.");
	}

	@Override
	public VertexTraversal<?, ?, ?> hasType(VertexTraversal<?, ?, ?> traverser, Class<?> type) {
		return traverser.filter(vertex -> {
			Class<?> vertexType = resolve(vertex.getElement());
			return vertexType == type;
		});
	}

	@Override
	public EdgeTraversal<?, ?, ?> hasType(EdgeTraversal<?, ?, ?> traverser, Class<?> type) {
		return traverser.filter(edge -> {
			Class<?> edgeType = resolve(edge.getElement());
			return edgeType == type;
		});
	}

}
