package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;

public interface DateGraphFieldList extends ListGraphField<DateGraphField, DateFieldListImpl> {

	public static final String TYPE = "date";

	/**
	 * Add another graph field to the list of graph fields.
	 * 
	 * @param key
	 * @return
	 */
	DateGraphField createDate(String key);

	/**
	 * Return the date field at the given index of the list.
	 * 
	 * @param index
	 * @return
	 */
	DateGraphField getDate(int index);
}
