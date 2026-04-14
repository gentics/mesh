package com.gentics.mesh.plugin.forms.model;

/**
 * Input element
 */
@Reportable
@FormElementType({"string", "number"})
public class InputElement extends FormElement {

	public InputElement() {
	}

	protected InputElement(InputElement other, boolean deep) {
		super(other, deep);
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new InputElement(this, deep);
	}
}
