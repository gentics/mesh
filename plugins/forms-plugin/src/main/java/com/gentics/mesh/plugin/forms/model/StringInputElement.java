package com.gentics.mesh.plugin.forms.model;

/**
 * String input element
 */
@Reportable
@FormElementType("string")
public class StringInputElement extends FormElement {

	public StringInputElement() {
	}

	protected StringInputElement(StringInputElement other, boolean deep) {
		super(other, deep);
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new StringInputElement(this, deep);
	}
}
