package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.NumberField;

public interface NumberGraphField extends ListableGraphField, BasicGraphField<NumberField> {

	/**
	 * Set the number in the graph field.
	 * 
	 * @param number
	 */
	public void setNumber(String number);

	/**
	 * Return the number that is stored in the graph field.
	 * 
	 * @return
	 */
	public String getNumber();
}
