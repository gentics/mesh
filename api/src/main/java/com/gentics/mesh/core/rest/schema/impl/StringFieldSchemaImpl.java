package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public class StringFieldSchemaImpl extends AbstractFieldSchema implements StringFieldSchema {

	@JsonProperty("allow")
	private String[] allowedValues;

	@Override
	public String getType() {
		return FieldTypes.STRING.toString();
	}

	@Override
	public String[] getAllowedValues() {
		return allowedValues;
	}

	@Override
	public StringFieldSchema setAllowedValues(String... allowedValues) {
		this.allowedValues = allowedValues;
		return this;
	}

	@Override
	public SchemaChangeModel compareTo(FieldSchema fieldSchema) throws IOException {
		SchemaChangeModel change = super.compareTo(fieldSchema);
		if (fieldSchema instanceof StringFieldSchema) {
			StringFieldSchema stringFieldSchema = (StringFieldSchema) fieldSchema;

			// allow
			if (!Arrays.equals(getAllowedValues(), stringFieldSchema.getAllowedValues())) {
				change.setOperation(UPDATEFIELD);
				change.getProperties().put(SchemaChangeModel.ALLOW_KEY, stringFieldSchema.getAllowedValues());
			}
		} else {
			return createTypeChange(fieldSchema);
		}

		return change;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		if (fieldProperties.get("allowedValues") != null) {
			setAllowedValues((String[]) fieldProperties.get("allowedValues"));
		}
	}

}
