package com.gentics.cailun.core.rest.tag.response;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;
import com.gentics.cailun.core.rest.user.response.UserResponse;

public class TagResponse extends AbstractRestModel {

	private String name;

	@JsonProperty("language")
	private String languageTag;

	private UserResponse creator;
	private Map<String, String> properties = new HashMap<>();

	public TagResponse() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public UserResponse getCreator() {
		return creator;
	}

	public void setCreator(UserResponse creator) {
		this.creator = creator;
	}

}
