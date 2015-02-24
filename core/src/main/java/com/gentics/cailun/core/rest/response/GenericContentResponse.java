package com.gentics.cailun.core.rest.response;

import java.util.Properties;

import org.codehaus.jackson.annotate.JsonProperty;

public class GenericContentResponse {

	private RestUser author;
	private Properties properties = new Properties();

	@JsonProperty("language")
	private String languageTag;

	public GenericContentResponse() {
	}

	public RestUser getAuthor() {
		return author;
	}

	public void setAuthor(RestUser author) {
		this.author = author;
	}

	public Properties getProperties() {
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

}
