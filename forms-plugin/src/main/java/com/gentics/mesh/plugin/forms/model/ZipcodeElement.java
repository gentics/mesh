package com.gentics.mesh.plugin.forms.model;

import io.vertx.core.MultiMap;

/**
 * Zipcode element
 */
@Reportable
public class ZipcodeElement extends FormElement {
	@Override
	public String getSingleFormData(MultiMap postedData) {
		return String.format("%s %s", postedData.get(getName()), postedData.get(String.format("%s_ort", getName()))).trim();
	}
}
