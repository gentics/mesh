package com.gentics.mesh.core.rest.schema.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

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
	public Optional<SchemaChangeModel> compareTo(FieldSchema fieldSchema) throws IOException {
		if (fieldSchema instanceof StringFieldSchema) {
			StringFieldSchema stringFieldSchema = (StringFieldSchema) fieldSchema;

			boolean modified = false;
			SchemaChangeModel change = SchemaChangeModel.createUpdateFieldChange(fieldSchema.getName());

			// required
			if (isRequired() != stringFieldSchema.isRequired()) {
				change.setProperty(SchemaChangeModel.REQUIRED_KEY, fieldSchema.isRequired());
				modified = true;
			}

			// allow
			if (!Arrays.equals(getAllowedValues(), stringFieldSchema.getAllowedValues())) {
				change.getProperties().put(SchemaChangeModel.ALLOW_KEY, stringFieldSchema.getAllowedValues());
				modified = true;
			}

			if (modified) {
				change.loadMigrationScript();
				return Optional.of(change);
			}
		} else {
			return createTypeChange(fieldSchema);
		}

		return Optional.empty();
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		if (fieldProperties.get("allowedValues") != null) {
			setAllowedValues((String[]) fieldProperties.get("allowedValues"));
		}
	}

}
