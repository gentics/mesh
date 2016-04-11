package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

@FunctionalInterface
public interface FieldUpdater {

	void update(GraphFieldContainer container, InternalActionContext ac, String fieldKey, Field restField, FieldSchema fieldSchema,
			FieldSchemaContainer schema);
}
