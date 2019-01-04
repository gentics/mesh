package com.gentics.mesh.graphdb.model;

import org.apache.tinkerpop.gremlin.structure.Element;

import com.gentics.madl.wrapper.element.WrappedElement;

/**
 * Basic interface for graph elements.
 */
public interface MeshElement extends WrappedElement {

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

}
