package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

public interface MicronodeGraphField extends ListableReferencingGraphField {
	/**
	 * Returns the micronode for this field.
	 * 
	 * @return Micronode for this field when set, otherwise null.
	 */
	Micronode getMicronode();

	/**
	 * Transform the graph field into a rest field.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param handler
	 */
	Observable<? extends Field> transformToRest(InternalActionContext ac, String fieldKey);
}
