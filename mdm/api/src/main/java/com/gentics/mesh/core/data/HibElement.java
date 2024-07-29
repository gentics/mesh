package com.gentics.mesh.core.data;

/**
 * Marker interface for domain elements. 
 * 
 * TODO MDM check whether this can be merged with {@link HibBaseElement}
 */
public interface HibElement {

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
	 * @return The id of this element.
	 */
	Object getId();

	/**
	 * Return the internal element version.
	 * 
	 * @return
	 */
	String getElementVersion();
}
