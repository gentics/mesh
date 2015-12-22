package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.ActionContext;

import rx.Observable;

public interface BasicGraphField<T extends Field> extends MicroschemaListableGraphField {

	/**
	 * Transform the graph field to the rest field.
	 * 
	 * @param ac
	 */
	Observable<T> transformToRest(ActionContext ac);

}
