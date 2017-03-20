package com.gentics.mesh.graphdb.model;

import com.tinkerpop.blueprints.Element;

/**
 * Basic interface for graph elements.
 */
public interface MeshElement {

	/**
	 * Set the uuid of the element.
	 * 
	 * @param uuid
	 *            Uuid of the element
	 */
	void setUuid(String uuid);

	/**
	 * Return the uuid of the element.
	 * 
	 * @return Uuid of the element
	 */
	String getUuid();

	/**
	 * Return the underlying graph element.
	 * 
	 * @return Graph element
	 */
	Element getElement();

	/**
	 * Reload the element from the graph. This is useful when you want to update the element which was updated within a different transaction but the reference
	 * to the element was reused.
	 */
	void reload();

}
