package com.gentics.mesh.plugin.forms.model;

/**
 * Number input element
 */
@Reportable
@FormElementType("number")
public class NumberInputElement extends FormElement {

	public NumberInputElement() {
	}

	protected NumberInputElement(NumberInputElement other, boolean deep) {
		super(other, deep);
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new NumberInputElement(this, deep);
	}
}
