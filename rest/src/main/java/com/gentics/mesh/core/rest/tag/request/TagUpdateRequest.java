package com.gentics.mesh.core.rest.tag.request;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.schema.response.SchemaReference;

public class TagUpdateRequest extends AbstractRestModel {

	private SchemaReference schema;
	private long order = 0;

	private String language;
	private Map<String, String> properties = new HashMap<>();

	public TagUpdateRequest() {
	}

	/**
	 * Return all properties that were loaded.
	 * 
	 * @return
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Return the property value for the given key.
	 * 
	 * @param languageKey
	 * @param key
	 * @return
	 */
	public String getProperty(String key) {
		return properties.get(key);
	}

	/**
	 * Add a key value pair to the properties.
	 * 
	 * @param languageKey
	 * @param key
	 * @param value
	 */
	public void addProperty(String key, String value) {
		properties.put(key, value);
	}

	public long getOrder() {
		return order;
	}

	public void setOrder(long order) {
		this.order = order;
	}

	public SchemaReference getSchema() {
		return schema;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
}
