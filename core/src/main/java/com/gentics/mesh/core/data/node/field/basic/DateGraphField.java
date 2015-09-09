package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.DateField;

public interface DateGraphField extends ListableGraphField, BasicGraphField<DateField> {

	/**
	 * Set the date within the field.
	 * 
	 * @param date
	 */
	void setDate(String date);

	/**
	 * Return the date which is stored in the field.
	 * 
	 * @return
	 */
	String getDate();

}
