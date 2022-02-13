package com.gentics.mesh.plugin.forms.model;

import java.util.List;
import java.util.function.Function;

import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;

/**
 * Element for conditional containers
 */
public class ConditionalContainerElement extends FormElement {
	private String elementToCheck;

	private String valueToCheck;

	public ConditionalContainerElement() {
	}

	protected ConditionalContainerElement(ConditionalContainerElement other, boolean deep) {
		super(other, deep);

		this.elementToCheck = other.elementToCheck;
		this.valueToCheck = other.valueToCheck;
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
		return valueToCheck;
	}

	public void setValueToCheck(String valueToCheck) {
		this.valueToCheck = valueToCheck;
	}
}
