package com.gentics.mesh.core.data;

/**
 * An element possessing a string key, pointing to a field.of a container. 
 * 
 * @author plyhun
 *
 */
public interface HibFieldKeyElement {

	/**
	 * Set the graph field key.
	 * 
	 * @param key
	 */
	void setFieldKey(String key);

	/**
	 * Return the graph field key.
	 * 
	 * @return
	 */
	String getFieldKey();
}
