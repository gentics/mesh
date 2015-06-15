package com.gentics.mesh.core.rest.common.response;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.schema.response.SchemaReference;
import com.gentics.mesh.core.rest.user.response.UserResponse;

public class AbstractPropertyContainerModel extends AbstractRestModel {

	private Map<String, String> properties = new HashMap<>();

	private SchemaReference schema;

	private String[] perms = {};

	private long order = 0;
	private UserResponse creator;

	public AbstractPropertyContainerModel() {
	}

	public UserResponse getCreator() {
		return creator;
	}

	public void setCreator(UserResponse author) {
		this.creator = author;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	public SchemaReference getSchema() {
		return schema;
	}

	public long getOrder() {
		return order;
	}

	public void setOrder(long order) {
		this.order = order;
	}

	public String[] getPerms() {
		return perms;
	}

	public void setPerms(String... perms) {
		this.perms = perms;
	}

	/**
	 * Return all properties for all languages that were loaded.
	 * 
	 * @return
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Return the property for the given key.
	 * 
	 * @param key
	 * @return
	 */
	public String getProperty(String key) {
		return properties.get(key);

	}

	/**
	 * Add a language specific property to the set of properties.
	 * 
	 * @param languageKey
	 * @param key
	 * @param value
	 */
	public void addProperty(String key, String value) {
		properties.put(key, value);
	}

}
