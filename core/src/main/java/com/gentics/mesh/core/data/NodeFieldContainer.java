package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;

public interface NodeFieldContainer extends FieldContainer, MicroschemaFieldContainer {

	Field getRestField(String fieldKey, FieldSchema fieldSchema);
}
