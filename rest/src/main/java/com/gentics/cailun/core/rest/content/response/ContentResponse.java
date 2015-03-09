package com.gentics.cailun.core.rest.content.response;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.cailun.core.rest.common.response.AbstractRestModel;
import com.gentics.cailun.core.rest.user.response.UserResponse;

public class ContentResponse extends AbstractRestModel {

	private UserResponse author;
	private Map<String, String> properties = new HashMap<>();
	private String type;

	@JsonProperty("language")
	private String languageTag;

	public ContentResponse() {
	}

	public UserResponse getAuthor() {
		return author;
	}

	public void setAuthor(UserResponse author) {
		this.author = author;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void addProperty(String key, String value) {
		if (value != null) {
			this.properties.put(key, value);
		}
	}

	public String getLanguageTag() {
		return languageTag;
	}

	public void setLanguageTag(String languageTag) {
		this.languageTag = languageTag;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
