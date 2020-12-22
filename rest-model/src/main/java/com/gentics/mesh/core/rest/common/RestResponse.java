package com.gentics.mesh.core.rest.common;

/**
 * Common interface for entity REST responses.
 */
public interface RestResponse extends RestModel {

	/**
	 * Return the uuid.
	 * 
	 * @return Uuid
	 */
	String getUuid();

	/**
	 * Set the uuid.
	 * 
	 * @param uuid
	 *            Uuid to be set
	 */
	void setUuid(String uuid);

}
