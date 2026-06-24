package com.gentics.mesh.plugin.forms.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for entries of {@code options} in {@link ValueListElement} types.
 */
public class ValueListItem {

	@JsonProperty
	private String value;

	@JsonProperty
	private Map<String, String> label;

	public String getValue() {
		return value;
	}

	public ValueListItem setValue(String value) {
		this.value = value;
		return this;
	}

	public Map<String, String> getLabel() {
		return label;
	}

	public ValueListItem setLabel(Map<String, String> label) {
		this.label = label;
		return this;
	}
}
