package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;

public interface HibNumberFieldList extends HibMicroschemaListableField, HibListField<HibNumberField, NumberFieldListImpl, Number> {
	/**
	 * Create a new number graph field with the given value.
	 * 
	 * @param value
	 * @return
	 */
	HibNumberField createNumber(Number value);

	/**
	 * Return the graph number field at the given position.
	 * 
	 * @param index
	 * @return
	 */
	HibNumberField getNumber(int index);

}
