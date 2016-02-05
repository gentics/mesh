package com.gentics.mesh.core.rest.schema.impl;

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

public class BinaryFieldSchemaImpl extends AbstractFieldSchema implements BinaryFieldSchema {

	@JsonProperty("allow")
	private String[] allowedMimeTypes;

	@Override
	public String[] getAllowedMimeTypes() {
		return allowedMimeTypes;
	}

	@Override
	public BinaryFieldSchema setAllowedMimeTypes(String... allowedMimeTypes) {
		this.allowedMimeTypes = allowedMimeTypes;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.BINARY.toString();
	}

	@Override
	public Optional<SchemaChangeModel> compareTo(FieldSchema fieldSchema) {
		if (fieldSchema instanceof BinaryFieldSchema) {
			BinaryFieldSchema binaryFieldSchema = (BinaryFieldSchema) fieldSchema;

			boolean modified = false;
			SchemaChangeModel change = new SchemaChangeModel(SchemaChangeOperation.UPDATEFIELD, fieldSchema.getName());

			// required
			modified = compareRequiredField(change, binaryFieldSchema, modified);

			// allow
			if (!Arrays.equals(getAllowedMimeTypes(), binaryFieldSchema.getAllowedMimeTypes())) {
				change.getProperties().put("allow", binaryFieldSchema.getAllowedMimeTypes());
				modified = true;
			}

			if (modified) {
				return Optional.of(change);
			}
		} else {
			//Error?
		}
		return Optional.empty();
	}
}
