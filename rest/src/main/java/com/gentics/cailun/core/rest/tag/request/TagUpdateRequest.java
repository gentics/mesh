package com.gentics.cailun.core.rest.tag.request;

import java.util.HashMap;
import java.util.Map;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;
import com.gentics.cailun.core.rest.schema.response.SchemaReference;

public class TagUpdateRequest extends AbstractRestModel {

	private SchemaReference schema;
	private long order = 0;

	private Map<String, Map<String, String>> properties = new HashMap<>();

	public TagUpdateRequest() {
	}

	/**
	 * Return all properties for all languages that were loaded.
	 * 
	 * @return
	 */
	public Map<String, Map<String, String>> getProperties() {
		return properties;
	}

	/**
	 * Return the properties for the language with the given language key.
	 * 
	 * @param languageKey
	 * @return
	 */
	public Map<String, String> getProperties(String languageKey) {
		return properties.get(languageKey);
	}

	/**
	 * Return the language specific property for the given language and the given key.
	 * 
	 * @param languageKey
	 * @param key
	 * @return
	 */
	public String getProperty(String languageKey, String key) {
		Map<String, String> languageProperties = properties.get(languageKey);
		if (languageProperties == null) {
			return null;
		}
		return languageProperties.get(key);
	}

	/**
	 * Add a language specific property to the set of properties.
	 * 
	 * @param languageKey
	 * @param key
	 * @param value
	 */
	public void addProperty(String languageKey, String key, String value) {
		Map<String, String> map = properties.get(languageKey);
		if (map == null) {
			map = new HashMap<>();
			properties.put(languageKey, map);
		}
		if (value != null) {
			map.put(key, value);
		}
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
}
