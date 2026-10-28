package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Combi Element
 */
@Reportable
@FormElementType("catalog")
public class CatalogElement extends ValueListElement {

	/**
	 * When {@code true}, the end user may enter a free-text value that is not part of the
	 * predefined {@code selectOptions}.
	 */
	@JsonProperty("customOption")
	protected boolean customOption;

	public CatalogElement() {
	}

	protected CatalogElement(CatalogElement other, boolean deep) {
		super(other, deep);

		this.customOption = other.customOption;
	}

	/**
	 * Effective custom-option flag. Returns {@code true} when either the element's own
	 * {@code customOption} field is set, or the value arrives nested inside {@code formGridOptions}
	 * (mirrors {@link ValueListElement#getEffectiveSelectOptions()}).
	 */
	@JsonProperty("customOption")
	public boolean isCustomOption() {
		return customOption || Boolean.TRUE.equals(getFormGridOptions().getCustomOption());
	}

	public CatalogElement setCustomOption(boolean customOption) {
		this.customOption = customOption;
		return this;
	}

	@Override
	protected boolean allowsCustomValues() {
		return isCustomOption();
	}

	@Override
	public FormElement clone(boolean deep) {
		return new CatalogElement(this, deep);
	}

}
