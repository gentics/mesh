package com.gentics.mesh.plugin.formgenerator;

import com.gentics.mesh.plugin.formgenerator.model.FormElement;
import com.gentics.mesh.plugin.formgenerator.model.Reportable;

import io.vertx.core.MultiMap;

/**
 * Custom element
 */
@Reportable
public class CustomElement extends FormElement {

	@Override
	public String getSingleFormData(MultiMap postedData) {
		String value = String.format("custom %s", super.getSingleFormData(postedData));
		return value;
	}
}
