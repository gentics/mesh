package com.gentics.mesh.core.rest.schema.impl;

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

public class MicronodeFieldSchemaImpl extends AbstractFieldSchema implements MicronodeFieldSchema {

	@JsonProperty("allow")
	private String[] allowedMicroSchemas;

	@Override
	public String[] getAllowedMicroSchemas() {
		return allowedMicroSchemas;
	}

	@Override
	public void setAllowedMicroSchemas(String... allowedMicroSchemas) {
		this.allowedMicroSchemas = allowedMicroSchemas;
	}

	@Override
	public String getType() {
		return FieldTypes.MICRONODE.toString();
	}

	@Override
	public Optional<SchemaChangeModel> compareTo(FieldSchema fieldSchema) {
		if (fieldSchema instanceof MicronodeFieldSchema) {
			MicronodeFieldSchema micronodeFieldSchema = (MicronodeFieldSchema) fieldSchema;

			boolean modified = false;
			SchemaChangeModel change = new SchemaChangeModel(SchemaChangeOperation.UPDATEFIELD, fieldSchema.getName());

			// required flag:
			modified = compareRequiredField(change, micronodeFieldSchema, modified);

			// allow
			if (!Arrays.equals(getAllowedMicroSchemas(), micronodeFieldSchema.getAllowedMicroSchemas())) {
				change.getProperties().put("allow", micronodeFieldSchema.getAllowedMicroSchemas());
				modified = true;
			}

			if (modified) {
				return Optional.of(change);
			}
		} else {
			return createTypeChange(fieldSchema);
		}
		return Optional.empty();
	}
}
