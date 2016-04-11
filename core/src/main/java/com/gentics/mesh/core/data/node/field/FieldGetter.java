package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.rest.schema.FieldSchema;

@FunctionalInterface
public interface FieldGetter {
	GraphField get(GraphFieldContainer container, FieldSchema schema);
}
