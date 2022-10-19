package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Function;

/**
 * Element for conditional containers
 */
public class ConditionalContainerElement extends FormElement {

	@JsonProperty("element_to_check")
	private String elementToCheck;

	@JsonProperty("value_to_check")
	private String valueToCheck;

	public ConditionalContainerElement() {
	}

	protected ConditionalContainerElement(ConditionalContainerElement other, boolean deep) {
		super(other, deep);

		this.elementToCheck = other.elementToCheck;
		this.valueToCheck = other.valueToCheck;
	}

	/**
	 * Check whether child elements should be skipped for validation and
	 * saving data.
	 *
	 * @param postedData The posted form data
	 * @return {@code true} when child elements should be skipped for validation
	 * 		and saving data, and {@code false} otherwise.
	 */
	@JsonIgnore
	public boolean skipElements(MultiMap postedData) {
		if (StringUtils.isBlank(elementToCheck)) {
			log.warn("Configuration problem for {} {}: elementToCheck not specified. Element will act as a normal container without special logic.");

			return false;
		}

		return postedData.getAll(elementToCheck).stream()
			.map(v -> v == null ? "" : v.trim())
			.noneMatch(v -> v.equals(getValueToCheck()));
	}

	public String getElementToCheck() {
		return elementToCheck;
	}

	@Override
	public void addFieldSchema(FieldSchemaContainer container) {
	}

	@Override
	public void addField(FieldContainer container, MultiMap postedData, boolean errors) {
	}

	@Override
	public void addFormData(JsonObject object, FieldContainer container, Function<FormElement, String> key) {
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new ConditionalContainerElement(this, deep);
	}

	@Override
	public String getSingleFormData(MultiMap postedData) {
		return null;
	}

	@Override
	public List<String> getMultiFormData(MultiMap postedData) {
		return null;
	}

	public void setElementToCheck(String elementToCheck) {
		this.elementToCheck = elementToCheck;
	}

	public String getValueToCheck() {
		if (valueToCheck == null) {
			valueToCheck = "";
		}

		return valueToCheck;
	}

	public void setValueToCheck(String valueToCheck) {
		this.valueToCheck = valueToCheck == null ? "" : valueToCheck.trim();
	}
}
