package com.gentics.mesh.plugin.forms.model.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single selectable option mapped from {@code selectOptions} of a catalog/value-list element.
 * Used inside {@link FormDataProperty#getPossibleValues()}.
 */
public class PossibleValue {

	@JsonProperty("identifier")
	private String identifier;

	@JsonProperty("displayName")
	private String displayName;

	public PossibleValue() {
	}

	public PossibleValue(String identifier, String displayName) {
		this.identifier = identifier;
		this.displayName = displayName;
	}

	public String getIdentifier() {
		return identifier;
	}

	public PossibleValue setIdentifier(String identifier) {
		this.identifier = identifier;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public PossibleValue setDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}
}
