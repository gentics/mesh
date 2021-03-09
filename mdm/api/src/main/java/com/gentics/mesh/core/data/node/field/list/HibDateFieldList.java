package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;

public interface HibDateFieldList extends HibMicroschemaListableField, HibListField<HibDateField, DateFieldListImpl, Long> {

	/**
	 * Add another graph field to the list of graph fields.
	 * 
	 * @param date
	 *            Date to be set for the new field
	 * @return
	 */
	HibDateField createDate(Long date);

	/**
	 * Return the date field at the given index of the list.
	 * 
	 * @param index
	 * @return
	 */
	HibDateField getDate(int index);
}
