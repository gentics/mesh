package com.gentics.mesh.core.rest.node.field;

public interface NumberField extends ListableField, MicroschemaListableField {

	/**
	 * Return the number value.
	 * 
	 * @return
	 */
	String getNumber();

	/**
	 * Set the number field value.
	 * 
	 * @param number
	 * @return Fluent API
	 */
	NumberField setNumber(String number);

}
