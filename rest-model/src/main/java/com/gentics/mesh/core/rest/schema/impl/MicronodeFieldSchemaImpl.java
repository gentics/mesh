package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ALLOW_KEY;

import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public class MicronodeFieldSchemaImpl extends AbstractFieldSchema implements MicronodeFieldSchema {

	@JsonProperty("allow")
	@JsonPropertyDescription("Array of names of allowed microschemas.")
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
	public Map<String, Object> getAllChangeProperties() {
		Map<String, Object> map = super.getAllChangeProperties();
		// allow
		map.put(SchemaChangeModel.ALLOW_KEY, getAllowedMicroSchemas());
		return map;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		if (fieldProperties.get(SchemaChangeModel.ALLOW_KEY) != null) {
			Object[] objectArray = (Object[]) fieldProperties.get(ALLOW_KEY);
			String[] stringArray = Arrays.copyOf(objectArray, objectArray.length, String[].class);
			setAllowedMicroSchemas(stringArray);
		}
	}
}
