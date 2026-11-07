package com.gentics.mesh.plugin.forms.model;

/**
 * Time element — represents a time picker control.
 */
@Reportable
@FormElementType("time")
public class TimeElement extends FormElement {

	public TimeElement() {
	}

	protected TimeElement(TimeElement other, boolean deep) {
		super(other, deep);
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new TimeElement(this, deep);
	}
}
