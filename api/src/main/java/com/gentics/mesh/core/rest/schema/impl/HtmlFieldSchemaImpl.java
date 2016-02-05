package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;

import java.util.Optional;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public class HtmlFieldSchemaImpl extends AbstractFieldSchema implements HtmlFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.HTML.toString();
	}

	@Override
	public Optional<SchemaChangeModel> compareTo(FieldSchema fieldSchema) {
		if (fieldSchema instanceof HtmlFieldSchema) {
			SchemaChangeModel change = new SchemaChangeModel(UPDATEFIELD, getName());
			HtmlFieldSchema htmlFieldSchema = (HtmlFieldSchema) fieldSchema;

			if (compareRequiredField(change, htmlFieldSchema, false)) {
				return Optional.of(change);
			}
		} else {
			return createTypeChange(fieldSchema);
		}
		return Optional.empty();
	}
}
