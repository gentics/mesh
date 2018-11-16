package com.gentics.mesh.graphdb.model;

import com.syncleus.ferma.ElementFrame;
import com.tinkerpop.blueprints.Element;

/**
 * Basic interface for graph elements.
 */
public interface MeshElement extends ElementFrame {

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
	 * Return the internal element version.
	 * 
	 * @return
	 */
	String getElementVersion();

	/**
	 * Provide TP 3.x compliant method.
	 * 
	 * @return
	 */
	default <N> N id() {
		return getId();
	}

}
