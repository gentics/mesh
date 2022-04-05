package com.gentics.mesh.plugin.forms;

import com.gentics.mesh.plugin.forms.model.FormElement;
import com.gentics.mesh.plugin.forms.model.Reportable;
import io.vertx.core.MultiMap;

/**
 * Custom element
 */
@Reportable
public class CustomConfigElement extends FormElement {

	public CustomConfigElement() {
	}

	protected CustomConfigElement(CustomConfigElement other, boolean deep) {
		super(other, deep);
	}

	@Override
	public String getSingleFormData(MultiMap postedData) {
		String value = String.format("custom %s", super.getSingleFormData(postedData));
		return value;
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new CustomConfigElement(this, deep);
	}
}
