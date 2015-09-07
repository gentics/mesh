package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.ActionContext;

public interface BasicGraphField<T extends Field> extends MicroschemaListableGraphField {

	T transformToRest(ActionContext ac);

}
