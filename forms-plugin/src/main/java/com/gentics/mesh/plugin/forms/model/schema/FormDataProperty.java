package com.gentics.mesh.plugin.forms.model.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the data entry for a single form control inside {@link FormData#getProperties()}.
 *
 * <p>The {@code value} field covers all single-value controls (string, number, datetime, binary,
 * aggregate/container). The {@code values} field is used for multi-value controls ({@code isList=true}),
 * such as multi-selects or aggregate lists.</p>
 *
 * <p>All fields except {@code value}/{@code values} are optional.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormDataProperty {

	@JsonProperty("value")
	private Object value;

	@JsonProperty("values")
	private List<Object> values;

	@JsonProperty("validationMessage")
	private String validationMessage;

	@JsonProperty("visible")
	private Boolean visible;

	@JsonProperty("editable")
	private Boolean editable;

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public List<Object> getValues() {
		return values;
	}

	public void setValues(List<Object> values) {
		this.values = values;
	}

	public String getValidationMessage() {
		return validationMessage;
	}

	public void setValidationMessage(String validationMessage) {
		this.validationMessage = validationMessage;
	}

	public Boolean getVisible() {
		return visible;
	}

	public void setVisible(Boolean visible) {
		this.visible = visible;
	}

	public Boolean getEditable() {
		return editable;
	}

	public void setEditable(Boolean editable) {
		this.editable = editable;
	}
}
