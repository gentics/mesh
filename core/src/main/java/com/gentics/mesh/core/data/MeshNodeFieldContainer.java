package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;

public interface MeshNodeFieldContainer extends FieldContainer, MicroschemaFieldContainer {

	Field getRestField(String fieldKey, FieldSchema fieldSchema);
}
