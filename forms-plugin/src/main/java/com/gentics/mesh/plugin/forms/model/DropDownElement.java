package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Combi Element
 */
@Reportable
@FormElementType("catalog")
public class DropDownElement extends ValueListElement {


	@JsonProperty("customOption")
	private ValueListItem customOption;

	public DropDownElement() {
	}

	protected DropDownElement(DropDownElement other, boolean deep) {
		super(other, deep);

		this.customOption = other.customOption;
	}

	public ValueListItem getCustomOption() {
		return customOption;
	}

	public void setCustomOption(ValueListItem customOption) {
		this.customOption = customOption;
	}

	@Override
	public FormElement clone(boolean deep) {
		return new DropDownElement(this, deep);
	}


}
