package com.gentics.madl.traversal;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Element;

import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.FramedGraph;

public interface FramedTraversal<S, M> extends Traversal<S, M> {

	FramedGraph getGraph();

	default <T> T getFramedVertex(Class<T> kind, Object id) {
		return getGraph().getFramedVertex(kind, id);
	}

	default <T> T getFramedVertexExplicit(Class<T> kind, Object id) {
		return getGraph().getFramedVertexExplicit(kind, id);
	}

	default <T> Iterator<? extends T> getFramedVertices(final Class<T> kind) {
		return getGraph().getFramedVertices(kind);
	}

	default <T> Iterator<? extends T> getFramedVertices(final String key, final Object value, final Class<T> kind) {
		return getGraph().getFramedVertices(key, value, kind);
	}

	default <T> Iterator<? extends T> getFramedVerticesExplicit(final Class<T> kind) {
		return getFramedVerticesExplicit(kind);
	}

	default <T> Iterator<? extends T> getFramedVerticesExplicit(final String key, final Object value, final Class<T> kind) {
		return getGraph().getFramedVerticesExplicit(key, value, kind);
	}

	default <T> Iterator<? extends T> getFramedEdges(final Class<T> kind) {
		return getGraph().getFramedEdges(kind);
	}

	default <T> Iterator<? extends T> getFramedEdges(final String key, final Object value, final Class<T> kind) {
		return getGraph().getFramedEdges(key, value, kind);
	}

	default <T> Iterator<? extends T> getFramedEdgesExplicit(final Class<T> kind) {
		return getGraph().getFramedEdgesExplicit(kind);
	}

	default <T> Iterator<? extends T> getFramedEdgesExplicit(final String key, final Object value, final Class<T> kind) {
		return getFramedEdgesExplicit(key, value, kind);
	}

	default <T> Iterator<? extends T> frame(Iterator<? extends Element> pipeline, final Class<T> kind) {
		return getGraph().frame(pipeline, kind);
	}

	default <T> T frameNewElement(Element e, ClassInitializer<T> initializer) {
		return getGraph().frameNewElement(e, initializer);
	}

	default <T> T frameNewElement(Element e, Class<T> kind) {
		return getGraph().frameNewElement(e, kind);
	}

	default <T> T frameElement(Element e, Class<T> kind) {
		return getGraph().frameElement(e, kind);
	}

	default <T> T frameNewElementExplicit(Element e, ClassInitializer<T> initializer) {
		return getGraph().frameNewElementExplicit(e, initializer);
	}

	default <T> T frameNewElementExplicit(Element e, Class<T> kind) {
		return getGraph().frameNewElementExplicit(e, kind);
	}

	default <T> T frameElementExplicit(Element e, Class<T> kind) {
		return getGraph().frameElementExplicit(e, kind);
	}

	default <T> Iterator<? extends T> frameExplicit(Iterator<? extends Element> pipeline, final Class<T> kind) {
		return getGraph().frameExplicit(pipeline, kind);
	}
}
