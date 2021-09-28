package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.ActionContext;

public interface HibBasicField<T extends Field> {

	/**
	 * Transform the graph field to the rest field.
	 * 
	 * @param ac
	 */
	T transformToRest(ActionContext ac);
}
