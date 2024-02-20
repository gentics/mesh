package com.gentics.madl.ext.orientdb;

import org.apache.commons.lang.NotImplementedException;
import org.apache.tinkerpop.gremlin.orientdb.OrientElement;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedElement;

import com.gentics.madl.ElementTypeClassCache;
import com.orientechnologies.orient.core.record.OElement;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.typeresolvers.TypeResolver;


public class OrientDBTypeResolver implements TypeResolver {

	private final ElementTypeClassCache elementTypeCache;

	public OrientDBTypeResolver(String... packagePaths) {
		this.elementTypeCache = new ElementTypeClassCache(packagePaths);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Class<? extends T> resolve(Element element, Class<T> kind) {
		if (element instanceof WrappedElement) {
			element = ((WrappedElement<Element>) element).getBaseElement();
		}
		if (element instanceof OrientElement) {
			OrientElement orientVertex = (OrientElement) element;
			return ((OElement) orientVertex.getRecord()).getSchemaType().map(type -> (Class<T>) resolve(type.getName(), kind)).orElse(null);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> Class<? extends T> resolve(String type, Class<T> kind) {
		final Class<T> nodeKind = (Class<T>) this.elementTypeCache.forName(type);
		if (kind.isAssignableFrom(nodeKind) || kind.equals(VertexFrame.class) || kind.equals(EdgeFrame.class)
			|| kind.equals(AbstractVertexFrame.class) || kind.equals(AbstractEdgeFrame.class) || kind.equals(Object.class)) {
			return nodeKind;
		} else {
			return kind;
		}
	}

	@Override
	public Class<?> resolve(Element element) {
		if (element instanceof OrientElement) {
			OrientElement orientVertex = (OrientElement) element;
			return ((OElement) orientVertex.getRecord()).getSchemaType().map(type -> elementTypeCache.forName(type.getName())).orElse(null);
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
	public <P extends Element, T extends Element> GraphTraversal<P,T> hasType(GraphTraversal<P,T> traverser, Class<?> type) {
		return traverser.filter(vertex -> {
			Class<?> vertexType = resolve(vertex.get());
			return vertexType == type;
		});
	}

	@Override
	public <P extends Element, T extends Element> GraphTraversal<P,T> hasNotType(GraphTraversal<P,T> traverser, Class<?> type) {
		return traverser.filter(vertex -> {
			Class<?> vertexType = resolve(vertex.get());
			return vertexType != type;
		});
	}
}
