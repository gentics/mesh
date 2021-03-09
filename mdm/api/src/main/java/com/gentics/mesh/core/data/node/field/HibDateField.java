package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.DateField;

public interface HibDateField extends HibListableField, HibBasicField<DateField> {

	/**
	 * Set the date within the field.
	 * 
	 * @param date
	 */
	void setDate(Long date);

	/**
	 * Return the date which is stored in the field.
	 * 
	 * @return
	 */
	Long getDate();

}
