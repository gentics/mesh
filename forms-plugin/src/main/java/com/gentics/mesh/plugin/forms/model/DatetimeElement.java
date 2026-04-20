package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Datetime element — represents a date/time picker control.
 * When {@code dateOnly} is {@code true}, only a date can be selected (no time component).
 */
@Reportable
@FormElementType("datetime")
public class DatetimeElement extends FormElement {

	
	private boolean dateOnly;

	public DatetimeElement() {
	}

	protected DatetimeElement(DatetimeElement other, boolean deep) {
		super(other, deep);
		this.dateOnly = other.dateOnly;
	}

	/**
	 * Whether only a date (without time) can be selected.
	 * @return dateOnly flag
	 */
	@JsonProperty("dateOnly")
	public boolean isDateOnly() {
		return dateOnly;
	}

	/**
	 * Set the dateOnly flag.
	 * @param dateOnly dateOnly flag
	 */
	public void setDateOnly(boolean dateOnly) {
		this.dateOnly = dateOnly;
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new DatetimeElement(this, deep);
	}
}
