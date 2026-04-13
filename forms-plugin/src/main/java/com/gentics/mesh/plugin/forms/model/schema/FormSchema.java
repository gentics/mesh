package com.gentics.mesh.plugin.forms.model.schema;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.plugin.forms.model.FormElement;

/**
 * Top-level schema wrapper — contains the data-model definitions of all form controls.
 * Corresponds to the "schema" field in the new form JSON format published by the CMS.
 * Each entry in {@code properties} is keyed by the control's identifier and deserialized
 * directly into the appropriate {@link FormElement} subclass.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormSchema {

	@JsonProperty("key")
	private String key;

	@JsonProperty("version")
	private String version;

	@JsonProperty("properties")
	private Map<String, FormElement> properties = new LinkedHashMap<>();

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Map<String, FormElement> getProperties() {
		
		return properties;
	}

	public void setProperties(Map<String, FormElement> properties) {
		this.properties = properties;
	}
}
