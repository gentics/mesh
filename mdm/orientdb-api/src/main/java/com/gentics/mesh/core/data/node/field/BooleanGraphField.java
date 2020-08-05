package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.BooleanField;

/**
 * The BooleanField Domain Model interface.
 * 
 * A boolean graph field is a basic node field which can be used to store boolean values.
 */
public interface BooleanGraphField extends ListableGraphField, BasicGraphField<BooleanField> {

	/**
	 * Return the boolean field value.
	 * 
	 * @return
	 */
	Boolean getBoolean();

	/**
	 * Set the boolean field value.
	 * 
	 * @param bool
	 */
	void setBoolean(Boolean bool);

}
