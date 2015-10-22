package com.gentics.mesh.core.rest.node.field;

public interface DateField extends ListableField, MicroschemaListableField {

	/**
	 * Return the date in the form of a epoch date.
	 * 
	 * @return Epoch date value
	 */
	Long getDate();

	/**
	 * Set the data in the form of a epoch date.
	 * 
	 * @param date
	 *            Epoch date value
	 * @return Fluent API
	 */
	DateField setDate(Long date);

}
