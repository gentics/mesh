package com.gentics.mesh.plugin.forms.model.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the pre-filled or current data state of a form.
 * Corresponds to the "data" field in the new form JSON format published by the CMS.
 *
 * <p>{@link #properties} is keyed by control identifier (UUID) and maps to
 * the current value/state of each control. The plugin stores and passes through
 * this data without evaluating it.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormData {

	@JsonProperty("formID")
	private String formID;

	@JsonProperty("version")
	private String version;

	@JsonProperty("hasValidationErrors")
	private boolean hasValidationErrors;

	@JsonProperty("properties")
	private Map<String, FormDataProperty> properties = new LinkedHashMap<>();

	public String getFormID() {
		return formID;
	}

	public void setFormID(String formID) {
		this.formID = formID;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isHasValidationErrors() {
		return hasValidationErrors;
	}

	public void setHasValidationErrors(boolean hasValidationErrors) {
		this.hasValidationErrors = hasValidationErrors;
	}

	public Map<String, FormDataProperty> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, FormDataProperty> properties) {
		this.properties = properties;
	}
}
