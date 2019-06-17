package com.gentics.mesh.core.rest.node.field;

public interface NumberField extends ListableField, MicroschemaListableField {

	/**
	 * Return the number value.
	 * 
	 * @return Number value
	 */
	Number getNumber();

	/**
	 * Set the number field value.
	 * 
	 * @param number
	 *            Number value
	 * @return Fluent API
	 */
	NumberField setNumber(Number number);

	@Override
	default Object getValue() {
		return getNumber();
	}
}
