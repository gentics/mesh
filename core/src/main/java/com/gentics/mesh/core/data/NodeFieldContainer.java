package com.gentics.mesh.core.data;

import io.vertx.ext.web.RoutingContext;

import java.util.Map;

import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.error.MeshSchemaException;

public interface NodeFieldContainer extends FieldContainer, MicroschemaFieldContainer {

	Field getRestField(String fieldKey, FieldSchema fieldSchema);

	void setFieldFromRest(RoutingContext rc, Map<String, Field> fields, Schema schema) throws MeshSchemaException;
}
