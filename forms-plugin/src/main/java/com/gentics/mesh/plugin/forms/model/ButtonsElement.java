package com.gentics.mesh.plugin.forms.model;

import java.util.List;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

import io.vertx.core.MultiMap;

/**
 * Buttons element. Generates no schema field and provides no data
 */
public class ButtonsElement extends FormElement {

	@Override
	public void addFieldSchema(FieldSchemaContainer container) {
	}

	@Override
	public String getSingleFormData(MultiMap postedData) {
		return null;
	}

	@Override
	public List<String> getMultiFormData(MultiMap postedData) {
		return null;
	}
}
