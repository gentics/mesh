package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.rest.node.field.FieldModel;
import com.gentics.mesh.handler.ActionContext;

public interface BasicField<T extends FieldModel> {

	/**
	 * Transform the graph field to the rest field.
	 * 
	 * @param ac
	 */
	T transformToRest(ActionContext ac);
}
