package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.BooleanField;

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
