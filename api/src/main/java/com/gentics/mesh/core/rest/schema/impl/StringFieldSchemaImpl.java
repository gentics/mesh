package com.gentics.mesh.core.rest.schema.impl;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModelImpl;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

public class StringFieldSchemaImpl extends AbstractFieldSchema implements StringFieldSchema {
	
	@JsonProperty("allow")
	private String[] allowedValues;

	@Override
	public String getType() {
		return FieldTypes.STRING.toString();
	}

	@Override
	public Optional<SchemaChangeModelImpl> compareTo(FieldSchema fieldSchema) {
		if (fieldSchema instanceof StringFieldSchema) {
			StringFieldSchema stringFieldSchema = (StringFieldSchema) fieldSchema;
			if (isRequired() != stringFieldSchema.isRequired()) {
				return Optional.of(new SchemaChangeModelImpl().setOperation(SchemaChangeOperation.UPDATEFIELD));
			}
		} else {
			//TODO type change 
		}

		return Optional.empty();
	}

	@Override
	public String[] getAllowedValues() {
		return allowedValues;
	}

	@Override
	public void setAllowedValues(String[] allowedValues) {
		this.allowedValues = allowedValues;
	}
}
