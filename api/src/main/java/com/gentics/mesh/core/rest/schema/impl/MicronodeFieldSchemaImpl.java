package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public class MicronodeFieldSchemaImpl extends AbstractFieldSchema implements MicronodeFieldSchema {

	@JsonProperty("allow")
	private String[] allowedMicroSchemas;

	@Override
	public String[] getAllowedMicroSchemas() {
		return allowedMicroSchemas;
	}

	@Override
	public MicronodeFieldSchema setAllowedMicroSchemas(String... allowedMicroSchemas) {
		this.allowedMicroSchemas = allowedMicroSchemas;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.MICRONODE.toString();
	}

	@Override
	public SchemaChangeModel compareTo(FieldSchema fieldSchema) throws IOException {
		SchemaChangeModel change = super.compareTo(fieldSchema);
		if (fieldSchema instanceof MicronodeFieldSchema) {
			MicronodeFieldSchema micronodeFieldSchema = (MicronodeFieldSchema) fieldSchema;

			// allow
			if (!Arrays.equals(getAllowedMicroSchemas(), micronodeFieldSchema.getAllowedMicroSchemas())) {
				change.setOperation(UPDATEFIELD);
				change.getProperties().put(SchemaChangeModel.ALLOW_KEY, micronodeFieldSchema.getAllowedMicroSchemas());
			}

		} else {
			return createTypeChange(fieldSchema);
		}
		return change;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		if (fieldProperties.get(SchemaChangeModel.ALLOW_KEY) != null) {
			setAllowedMicroSchemas((String[]) fieldProperties.get(SchemaChangeModel.ALLOW_KEY));
		}
	}
}
