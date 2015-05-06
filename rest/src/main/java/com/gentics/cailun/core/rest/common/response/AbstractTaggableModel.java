package com.gentics.cailun.core.rest.common.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.cailun.core.rest.schema.response.SchemaReference;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.core.rest.user.response.UserResponse;

public class AbstractTaggableModel extends AbstractRestModel {

	private Map<String, Map<String, String>> properties = new HashMap<>();

	private List<TagResponse> tags = new ArrayList<>();
	private SchemaReference schema;

	private String[] perms = {};

	private long order = 0;
	private UserResponse creator;
	private String parentTagUuid;

	public AbstractTaggableModel() {
	}

	public UserResponse getCreator() {
		return creator;
	}

	public void setCreator(UserResponse author) {
		this.creator = author;
	}

	public List<TagResponse> getTags() {
		return tags;
	}

	public void setTags(List<TagResponse> tags) {
		this.tags = tags;
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

	public String getParentTagUuid() {
		return parentTagUuid;
	}

	public void setParentTagUuid(String parentTagUuid) {
		this.parentTagUuid = parentTagUuid;
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

}
