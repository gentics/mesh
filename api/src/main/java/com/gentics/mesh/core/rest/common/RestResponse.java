package com.gentics.mesh.core.rest.common;

public interface RestResponse extends RestModel {

	/**
	 * Return the uuid.
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Set the uuid.
	 * 
	 * @param uuid
	 */
	void setUuid(String uuid);

}
