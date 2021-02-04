package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;

public interface HibStringFieldList extends HibMicroschemaListableField, HibListField<HibStringField, StringFieldListImpl, String> {

	/**
	 * Create a new string field and add it to the list.
	 * 
	 * @param string
	 * @return
	 */
	HibStringField createString(String string);

	/**
	 * Return the string item at the given position.
	 * 
	 * @param index
	 * @return
	 */
	HibStringField getString(int index);

}
