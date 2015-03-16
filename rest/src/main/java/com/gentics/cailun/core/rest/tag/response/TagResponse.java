package com.gentics.cailun.core.rest.tag.response;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;
import com.gentics.cailun.core.rest.user.response.UserResponse;

public class TagResponse extends AbstractRestModel {

	private String type;
	private long order = 0;

	private UserResponse creator;
	private Map<String, Map<String, String>> properties = new HashMap<>();

	public TagResponse() {
	}

	public Map<String, Map<String, String>> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Map<String, String>> properties) {
		this.properties = properties;
	}

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
