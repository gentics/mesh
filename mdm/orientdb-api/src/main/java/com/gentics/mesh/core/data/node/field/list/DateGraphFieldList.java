package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;

/**
 * Domain model definition for a date list.
 */
public interface DateGraphFieldList extends ListGraphField<DateGraphField, DateFieldListImpl, Long> {

	String TYPE = "date";

	/**
	 * Add another graph field to the list of graph fields.
	 * 
	 * @param date
	 *            Date to be set for the new field
	 * @return
	 */
	DateGraphField createDate(Long date);

	/**
	 * Return the date field at the given index of the list.
	 * 
	 * @param index
	 * @return
	 */
	DateGraphField getDate(int index);
}
