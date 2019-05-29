package com.gentics.mesh.util;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Result container which can be used to return information from within a transaction handler.
 */
public class ResultInfo {

	private RestModel model;

	Map<String, String> properties = new HashMap<>();

	/**
	 * Create a new result.
	 * 
	 * @param model
	 * @param batch
	 */
	public ResultInfo(RestModel model) {
		this.model = model;
	}

	/**
	 * Return the stored rest model for the result.
	 * 
	 * @return
	 */
	public RestModel getModel() {
		return model;
	}

	/**
	 * Set additional property values.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public ResultInfo setProperty(String key, String value) {
		properties.put(key, value);
		return this;
	}

	/**
	 * Return the property value for the given key.
	 * 
	 * @param key
	 * @return
	 */
	public String getProperty(String key) {
		return properties.get(key);
	}

}
