package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;

public interface StringGraphFieldList extends ListGraphField<StringGraphField, StringFieldListImpl, String> {

	String TYPE = "string";

	/**
	 * Create a new string field and add it to the list.
	 * 
	 * @param string
	 * @return
	 */
	StringGraphField createString(String string);

	/**
	 * Return the string item at the given position.
	 * 
	 * @param index
	 * @return
	 */
	StringGraphField getString(int index);

}
