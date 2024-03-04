package com.gentics.mesh.madl.frame;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;

import com.gentics.madl.graph.DelegatingFramedMadlGraph;

public interface ElementFrame extends com.syncleus.ferma.ElementFrame {

	static final String TYPE_RESOLUTION_KEY = "ferma_type";

	default Element element() {
		return getElement();
	}

	@Override
	DelegatingFramedMadlGraph<? extends Graph> getGraph();

	@SuppressWarnings("unchecked")
	default <T> T getGraphAttribute(String key) {
		return (T) getGraph().getAttribute(key);
	}
}
