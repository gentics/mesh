package com.gentics.mesh.graphdb.model;

import com.tinkerpop.blueprints.Element;

public interface MeshElement {

	void setUuid(String uuid);

	String getUuid();

	Element getElement();

	/**
	 * Reload the element from the graph. This is useful when you want to update the element which was updated within a different transaction but the reference
	 * to the element was reused.
	 */
	void reload();

}
