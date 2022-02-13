package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for entries of {@code options} in {@link ValueListElement} types.
 */
public class ValueListItem {

	@JsonProperty
	private String key;

	@JsonProperty
	private String value;

	public String getKey() {
		return key;
	}

	public ValueListItem setKey(String key) {
		this.key = key;

		return this;
	}

	public String getValue() {
		return value;
	}

	public ValueListItem setValue(String value) {
		this.value = value;

		return this;
	}
}
