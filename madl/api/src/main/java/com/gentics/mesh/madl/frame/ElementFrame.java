package com.gentics.mesh.madl.frame;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;

import com.gentics.madl.graph.DelegatingFramedMadlGraph;
import com.syncleus.ferma.WrappedFramedGraph;

public interface ElementFrame extends com.syncleus.ferma.ElementFrame {

	static final String TYPE_RESOLUTION_KEY = "ferma_type";

	/**
	 * Return the id of the element.
	 *
	 * @return The id of this element.
	 */
	default Object id() {
		return getId();
	}

	Element element();

	@Override
	DelegatingFramedMadlGraph<? extends Graph> getGraph();

	@SuppressWarnings("unchecked")
	default <T> T getGraphAttribute(String key) {
		return (T) ((WrappedFramedGraph<? extends Graph>) getGraph()).getBaseGraph().variables().get(key).orElseThrow();
	}
}
