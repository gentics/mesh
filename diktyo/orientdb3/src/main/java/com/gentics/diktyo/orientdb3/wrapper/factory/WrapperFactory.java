package com.gentics.diktyo.orientdb3.wrapper.factory;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.diktyo.orientdb3.wrapper.element.AbstractWrappedEdge;
import com.gentics.diktyo.orientdb3.wrapper.element.AbstractWrappedVertex;

public final class WrapperFactory {

	public static <R> R frameElement(Element element, Class<R> clazzOfR) {
		if (element == null) {
			return null;
		}
		if (element instanceof Vertex) {
			return frameVertex((Vertex) element, clazzOfR);
		}
		if (element instanceof Edge) {
			return frameEdge((Edge) element, clazzOfR);
		}
		throw new RuntimeException("Unknonwn type of element {" + element.getClass() + "}");
	}

	public static <R> R frameVertex(Vertex vertex, Class<R> clazzOfR) {
		if (vertex == null) {
			return null;
		}
		try {
			R element = clazzOfR.newInstance();
			if (element instanceof AbstractWrappedVertex) {
				((AbstractWrappedVertex) element).init(vertex);
			} else {
				throw new RuntimeException("The specified class {" + clazzOfR + "} does not use {" + AbstractWrappedVertex.class + "}");
			}
			return element;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Could not instantiate wrapper for class {" + clazzOfR.getName() + "}");
		}
	}

	public static <R> R frameEdge(Edge edge, Class<R> clazzOfR) {
		if (edge == null) {
			return null;
		}
		try {
			R element = clazzOfR.newInstance();
			if (element instanceof AbstractWrappedEdge) {
				((AbstractWrappedEdge) element).init(edge);
			} else {
				throw new RuntimeException("The specified class {" + clazzOfR + "} does not use {" + AbstractWrappedVertex.class + "}");
			}
			return element;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Could not instantiate wrapper for class {" + clazzOfR.getName() + "}");
		}
	}

}
