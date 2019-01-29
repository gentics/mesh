package com.gentics.mesh.util;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Result container which can be used to return information from within a transaction handler.
 */
public class ResultInfo {

	private RestModel model;

	private EventQueueBatch batch;

	Map<String, String> properties = new HashMap<>();

	/**
	 * Create a new result.
	 * 
	 * @param model
	 * @param batch
	 */
	public ResultInfo(RestModel model, EventQueueBatch batch) {
		this.model = model;
		this.batch = batch;
	}

	/**
	 * Create a new result.
	 * 
	 * @param model
	 */
	public ResultInfo(EventQueueBatch batch) {
		this.batch = batch;
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
	 * Return the stored search queue batch.
	 * 
	 * @return
	 */
	public EventQueueBatch getBatch() {
		return batch;
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
