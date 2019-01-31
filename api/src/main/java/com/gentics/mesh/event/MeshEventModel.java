package com.gentics.mesh.event;

public interface MeshEventModel {

	/**
	 * Return the uuid of the element which the element references
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Return the mesh cluster node from which this event originated.
	 * 
	 * @return
	 */

	String getOrigin();

	/**
	 * Return the name of the referenced element.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Return the address of the event.
	 * 
	 * @return
	 */
	String getAddress();

}
