package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.NumberField;

public interface HibNumberField extends HibListableField, HibBasicField<NumberField> {

	/**
	 * Set the number in the graph field.
	 * 
	 * @param number
	 */
	public void setNumber(Number number);

	/**
	 * Return the number that is stored in the graph field.
	 * 
	 * @return
	 */
	public Number getNumber();
}
