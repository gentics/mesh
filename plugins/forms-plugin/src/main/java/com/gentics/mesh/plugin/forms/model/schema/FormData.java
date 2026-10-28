package com.gentics.mesh.plugin.forms.model.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

	private String formDataID;

	@JsonProperty("version")
	private String version;

	@JsonProperty("hasValidationErrors")
	private boolean hasValidationErrors;

	@JsonProperty("properties")
	private Map<String, FormDataProperty> properties = new LinkedHashMap<>();

	@JsonIgnore
	private String redirectUrl;

	public String getFormID() {
		return formID;
	}

	public FormData setFormID(String formID) {
		this.formID = formID;
		return this;
	}

	public String getFormDataID() {
		return formDataID;
	}

	public FormData setFormDataID(String formDataID) {
		this.formDataID = formDataID;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public FormData setVersion(String version) {
		this.version = version;
		return this;
	}

	public boolean isHasValidationErrors() {
		return hasValidationErrors;
	}

	public FormData setHasValidationErrors(boolean hasValidationErrors) {
		this.hasValidationErrors = hasValidationErrors;
		return this;
	}

	public Map<String, FormDataProperty> getProperties() {
		return properties;
	}

	public FormData setProperties(Map<String, FormDataProperty> properties) {
		this.properties = properties;
		return this;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public FormData setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
		return this;
	}
}
