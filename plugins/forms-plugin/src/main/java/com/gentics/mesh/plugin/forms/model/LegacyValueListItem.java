package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Old model for entries of {@code options} in {@link ValueListElement} types.
 *
 * @deprecated Use {@link ValueListElement} instead
 */
public class LegacyValueListItem {

	@JsonProperty
	private String label;

	@JsonProperty
	private String value;

	public String getLabel() {
		return label;
	}

	public LegacyValueListItem setLabel(String label) {
		this.label = label;

		return this;
	}

	public String getValue() {
		return value;
	}

	public LegacyValueListItem setValue(String value) {
		this.value = value;

		return this;
	}
}
