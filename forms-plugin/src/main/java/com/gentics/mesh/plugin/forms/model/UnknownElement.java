package com.gentics.mesh.plugin.forms.model;

import java.util.List;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.plugin.forms.model.schema.PostedData;

/**
 * Unknown element (posted data is ignored)
 */
public class UnknownElement extends FormElement {

	public UnknownElement() {
	}

	protected UnknownElement(UnknownElement other, boolean deep) {
		super(other, deep);
	}

	@Override
	public void addFieldSchema(FieldSchemaContainer container) {
	}

	@Override
	public String getSingleFormData(PostedData postedData) {
		return null;
	}

	@Override
	public List<String> getMultiFormData(PostedData postedData) {
		return null;
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new UnknownElement(this, deep);
	}
}
