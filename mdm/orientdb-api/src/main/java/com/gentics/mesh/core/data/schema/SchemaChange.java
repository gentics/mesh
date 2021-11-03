package com.gentics.mesh.core.data.schema;

import java.io.IOException;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

/**
 * A GraphDB counterpart for @link {@link HibSchemaChange}.
 */
public interface SchemaChange<T extends FieldSchemaContainer> extends MeshVertex, HibSchemaChange<T> {

	String REST_PROPERTY_PREFIX_KEY = "fieldProperty_";

	default SchemaChangeModel transformToRest() throws IOException {
		SchemaChangeModel model = HibSchemaChange.super.transformToRest();
		// Strip away the prefix
		for (String key : model.getProperties().keySet()) {
			Object value = model.getProperties().remove(key);
			key = key.replace(REST_PROPERTY_PREFIX_KEY, "");
			model.getProperties().put(key, value);
		}
		return model;
	}
}
