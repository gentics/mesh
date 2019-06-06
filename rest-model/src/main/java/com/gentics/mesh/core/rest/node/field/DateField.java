package com.gentics.mesh.core.rest.node.field;

/**
 * Date Fields can store dates in the epoch date format.
 */
public interface DateField extends ListableField, MicroschemaListableField {

	/**
	 * Return the date in the form of an ISO-8601 date.
	 * 
	 * @return ISO-8601 date
	 */
	String getDate();

	/**
	 * Set the data in the form of an ISO-8601 date.
	 * 
	 * @param date
	 *            ISO-8601 date
	 * @return Fluent API
	 */
	DateField setDate(String date);

	@Override
	default Object getValue() {
		return getDate();
	}
}
