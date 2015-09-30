package com.gentics.mesh.graphdb.model;

import com.tinkerpop.blueprints.Element;

/**
 * Basic interface for graph elements.
 *
 */
public interface MeshElement {

	/**
	 * Set the uuid of the element.
	 * 
	 * @param uuid
	 */
	void setUuid(String uuid);

	/**
	 * Return the uuid of the element.
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Return the underlying graph element.
	 * 
	 * @return
	 */
	Element getElement();

	/**
	 * Reload the element from the graph. This is useful when you want to update the element which was updated within a different transaction but the reference
	 * to the element was reused.
	 */
	void reload();

}
