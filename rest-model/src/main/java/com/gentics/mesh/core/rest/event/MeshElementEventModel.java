package com.gentics.mesh.core.rest.event;

public interface MeshElementEventModel extends MeshEventModel {

	/**
	 * Return the uuid of the element which the element references
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Set the uuid of the element.
	 * 
	 * @param uuid
	 * 
	 */
	void setUuid(String uuid);

	/**
	 * Return the name of the referenced element.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Set the name of the referenced element.
	 * 
	 * @param name
	 */
	void setName(String name);

}
