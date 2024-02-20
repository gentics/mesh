package com.gentics.mesh.madl.frame;

import org.apache.tinkerpop.gremlin.structure.Element;

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
}
