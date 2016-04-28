package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

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
	public SchemaChangeModel compareTo(FieldSchema fieldSchema) throws IOException {
		SchemaChangeModel change = super.compareTo(fieldSchema);
		if (fieldSchema instanceof BinaryFieldSchema) {
			BinaryFieldSchema binaryFieldSchema = (BinaryFieldSchema) fieldSchema;

			// allow
			if (!Arrays.equals(getAllowedMimeTypes(), binaryFieldSchema.getAllowedMimeTypes())) {
				change.setOperation(UPDATEFIELD);
				change.getProperties().put("allow", binaryFieldSchema.getAllowedMimeTypes());
			}

		} else {
			return createTypeChange(fieldSchema);
		}
		return change;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		if (fieldProperties.get("allowedMimeTypes") != null) {
			setAllowedMimeTypes((String[]) fieldProperties.get("allowedMimeTypes"));
		}
	}

}
