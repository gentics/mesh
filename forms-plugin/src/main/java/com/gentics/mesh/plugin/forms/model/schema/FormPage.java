package com.gentics.mesh.plugin.forms.model.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FormPage {

	@JsonProperty("pagename")
	private Map<String, String> pagename;

	@JsonProperty("elements")
	private List<UiSchemaPageElement> elements;

	public Map<String, String> getPagename() {
		return pagename;
	}

	public void setPagename(Map<String, String> pagename) {
		this.pagename = pagename;
	}

	public List<UiSchemaPageElement> getElements() {
		return elements;
	}

	public void setElements(List<UiSchemaPageElement> elements) {
		this.elements = elements;
	}
}
