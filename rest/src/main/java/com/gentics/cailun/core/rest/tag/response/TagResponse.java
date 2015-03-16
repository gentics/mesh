package com.gentics.cailun.core.rest.tag.response;

import java.util.HashMap;
import java.util.Map;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;
import com.gentics.cailun.core.rest.user.response.UserResponse;

public class TagResponse extends AbstractRestModel {

	private String schema;
	private long order = 0;

	private UserResponse creator;
	private Map<String, Map<String, String>> properties = new HashMap<>();

	private String[] perms;

	public TagResponse() {
	}

	/**
	 * Return all properties for all languages that were loaded.
	 * 
	 * @return
	 */
	public Map<String, Map<String, String>> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Map<String, String>> properties) {
		this.properties = properties;
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

	public UserResponse getCreator() {
		return creator;
	}

	public void setCreator(UserResponse creator) {
		this.creator = creator;
	}

	public long getOrder() {
		return order;
	}

	public void setOrder(long order) {
		this.order = order;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String[] getPerms() {
		return perms;
	}

	public void setPerms(String... perms) {
		this.perms = perms;
	}

}
