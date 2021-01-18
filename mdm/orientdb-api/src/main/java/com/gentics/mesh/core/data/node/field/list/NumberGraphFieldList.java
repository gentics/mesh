package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;

/**
 * List field domain class for numbers.
 */
public interface NumberGraphFieldList extends ListGraphField<NumberGraphField, NumberFieldListImpl, Number> {

	String TYPE = "number";

	/**
	 * Create a new number graph field with the given value.
	 * 
	 * @param value
	 * @return
	 */
	NumberGraphField createNumber(Number value);

	/**
	 * Return the graph number field at the given position.
	 * 
	 * @param index
	 * @return
	 */
	NumberGraphField getNumber(int index);

}
