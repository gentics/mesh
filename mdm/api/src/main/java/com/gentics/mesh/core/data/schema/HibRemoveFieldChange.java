package com.gentics.mesh.core.data.schema;

import java.util.Collections;
import java.util.Map;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Change entry which contains information for a field removal.
 */
public interface HibRemoveFieldChange extends HibSchemaFieldChange {

	SchemaChangeOperation OPERATION = SchemaChangeOperation.REMOVEFIELD;

	@Override
	default SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	@Override
	default Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldMap oldfields) {
		return Collections.emptyMap();
	}

	@Override
	default <R extends FieldSchemaContainer> R apply(R container) {
		container.removeField(getFieldName());
		return container;
	}
}
