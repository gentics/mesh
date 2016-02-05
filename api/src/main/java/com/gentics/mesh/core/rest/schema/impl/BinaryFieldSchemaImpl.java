package com.gentics.mesh.core.rest.schema.impl;

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModelImpl;
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
	public Optional<SchemaChangeModelImpl> compareTo(FieldSchema fieldSchema) {
		if (fieldSchema instanceof BinaryFieldSchemaImpl) {
			BinaryFieldSchemaImpl binaryField = (BinaryFieldSchemaImpl) fieldSchema;
			if (!Arrays.asList(getAllowedMimeTypes()).containsAll(Arrays.asList(binaryField.getAllowedMimeTypes()))) {
				SchemaChangeModelImpl change = new SchemaChangeModelImpl().setOperation(SchemaChangeOperation.UPDATEFIELD);
				change.getProperties().put("allowedMimeTypes", binaryField.getAllowedMimeTypes());
				return Optional.of(change);
			}
			System.out.println("sdegasdgas");
		} else {
			//Error?
		}
		return Optional.empty();
	}
}
