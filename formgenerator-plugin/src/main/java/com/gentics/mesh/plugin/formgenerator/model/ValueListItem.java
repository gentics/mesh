package com.gentics.mesh.plugin.formgenerator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for entries of <code>listelements</code> in {@link ValueListElement} types.
 */
public class ValueListItem {

	@JsonProperty
	private String label;

	@JsonProperty
	private String value;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
