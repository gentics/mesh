package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;

public interface BooleanGraphFieldList extends ListGraphField<BooleanGraphField, BooleanFieldListImpl, Boolean> {

	String TYPE = "boolean";

	/**
	 * Return the boolean graph field at index position.
	 * 
	 * @param index
	 * @return
	 */
	BooleanGraphField getBoolean(int index);

	/**
	 * Create a boolean graph field within the list.
	 * 
	 * @param flag
	 * @return
	 */
	BooleanGraphField createBoolean(Boolean flag);
}
