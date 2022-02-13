package com.gentics.mesh.plugin.forms.model;

/**
 * Textarea element
 */
@Reportable
public class TextareaElement extends FormElement {

	public TextareaElement() {
	}

	protected TextareaElement(TextareaElement other, boolean deep) {
		super(other, deep);
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new TextareaElement(this, deep);
	}
}
