package com.gentics.mesh.core.rest.schema.impl;

import java.io.IOException;
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
	public Optional<SchemaChangeModel> compareTo(FieldSchema fieldSchema) throws IOException {
		if (fieldSchema instanceof HtmlFieldSchema) {
			SchemaChangeModel change = SchemaChangeModel.createUpdateFieldChange(getName());
			HtmlFieldSchema htmlFieldSchema = (HtmlFieldSchema) fieldSchema;

			if (compareRequiredField(change, htmlFieldSchema, false)) {
				change.loadMigrationScript();
				return Optional.of(change);
			}
		} else {
			return createTypeChange(fieldSchema);
		}
		return Optional.empty();
	}
}
