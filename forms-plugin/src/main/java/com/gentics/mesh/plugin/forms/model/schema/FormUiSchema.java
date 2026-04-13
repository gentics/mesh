package com.gentics.mesh.plugin.forms.model.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * UI schema wrapper — contains the layout and presentation definitions for rendering a form.
 * Corresponds to the "uiSchema" field in the new form JSON format published by the CMS.
 * The plugin stores and passes through this data without evaluating it; all page/grid logic
 * is handled by the frontend.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormUiSchema {

	@JsonProperty("key")
	private String key;

	@JsonProperty("version")
	private String version;

	@JsonProperty("formname")
	private Map<String, String> formname;

	@JsonProperty("formdescription")
	private Map<String, String> formdescription;

	@JsonProperty("formwidth")
	private String formwidth;

	@JsonProperty("formFlowKey")
	private String formFlowKey;

	@JsonProperty("pages")
	private List<Map<String, Object>> pages;

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

	public Map<String, String> getFormname() {
		return formname;
	}

	public void setFormname(Map<String, String> formname) {
		this.formname = formname;
	}

	public Map<String, String> getFormdescription() {
		return formdescription;
	}

	public void setFormdescription(Map<String, String> formdescription) {
		this.formdescription = formdescription;
	}

	public String getFormwidth() {
		return formwidth;
	}

	public void setFormwidth(String formwidth) {
		this.formwidth = formwidth;
	}

	public String getFormFlowKey() {
		return formFlowKey;
	}

	public void setFormFlowKey(String formFlowKey) {
		this.formFlowKey = formFlowKey;
	}

	public List<Map<String, Object>> getPages() {
		return pages;
	}

	public void setPages(List<Map<String, Object>> pages) {
		this.pages = pages;
	}
}
