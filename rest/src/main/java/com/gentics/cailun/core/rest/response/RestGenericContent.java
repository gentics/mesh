package com.gentics.cailun.core.rest.response;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

public class RestGenericContent extends AbstractRestModel {

	private RestUser author;
	private Map<String, String> properties = new HashMap<>();
	private String type;

	@JsonProperty("language")
	private String languageTag;

	public RestGenericContent() {
	}

	public RestUser getAuthor() {
		return author;
	}

	public void setAuthor(RestUser author) {
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
