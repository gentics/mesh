package com.gentics.mesh.plugin.forms.model;

import java.util.List;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

import io.vertx.core.MultiMap;

/**
 * Unknown element (posted data is ignored)
 */
public class UnknownElement extends FormElement {
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
