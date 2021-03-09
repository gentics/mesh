package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;

public interface HibBooleanFieldList extends HibMicroschemaListableField, HibListField<HibBooleanField, BooleanFieldListImpl, Boolean> {

	/**
	 * Return the boolean graph field at index position.
	 * 
	 * @param index
	 * @return
	 */
	HibBooleanField getBoolean(int index);

	/**
	 * Create a boolean graph field within the list.
	 * 
	 * @param flag
	 * @return
	 */
	HibBooleanField createBoolean(Boolean flag);
}
