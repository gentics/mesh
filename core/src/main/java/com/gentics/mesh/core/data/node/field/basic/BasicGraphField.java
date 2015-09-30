package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.ActionContext;

public interface BasicGraphField<T extends Field> extends MicroschemaListableGraphField {

	/**
	 * Transform the graph field to the rest field.
	 * 
	 * @param ac
	 * @return
	 */
	T transformToRest(ActionContext ac);

}
