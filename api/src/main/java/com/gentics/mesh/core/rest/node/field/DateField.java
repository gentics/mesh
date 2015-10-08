package com.gentics.mesh.core.rest.node.field;

public interface DateField extends ListableField, MicroschemaListableField {

	/**
	 * Return the date in the form of a epoch date.
	 * 
	 * @return
	 */
	Long getDate();

	/**
	 * Set the data in the form of a epoch date.
	 * 
	 * @param date
	 * @return
	 */
	DateField setDate(Long date);

}
