package com.gentics.mesh.core.data;

public interface HibElement {

	/**
	 * UUID of the element.
	 * @return
	 */
	String getUuid();

	/**
	 * Id of the element. This is legacy support method which is used to handle perm checks. 
	 * 
	 * @return
	 */
	Object getId();

}
