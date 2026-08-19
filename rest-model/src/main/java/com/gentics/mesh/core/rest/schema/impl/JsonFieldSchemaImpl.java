package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ALLOW_KEY;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.JsonSchema;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.JsonFieldSchema;

import io.vertx.core.json.JsonArray;

/**
 * @see JsonFieldSchema
 */
public class JsonFieldSchemaImpl extends AbstractFieldSchema implements JsonFieldSchema {

	@JsonProperty("allow")
	@JsonPropertyDescription("List of allowed JSON schemas, the data of this field must match.")
	private JsonSchema[] allowedSchemas;

	@Override
	public String getType() {
		return FieldTypes.JSON.toString();
	}

	@Override
	public JsonSchema[] getAllowedSchemas() {
		return allowedSchemas;
	}

	@Override
	public JsonFieldSchemaImpl setAllowedSchemas(JsonSchema... allowedSchemas) {
		this.allowedSchemas = allowedSchemas;
		return this;
	}

	@Override
	public Map<String, Object> getAllChangeProperties() {
		Map<String, Object> map = super.getAllChangeProperties();
		map.put(ALLOW_KEY, getAllowedSchemas());
		return map;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		Object allowedValues = fieldProperties.get(ALLOW_KEY);
		if (allowedValues != null) {
			if (allowedValues instanceof JsonSchema[] allowedSchemas) {
				setAllowedSchemas(allowedSchemas);
			} else if (allowedValues instanceof String[]) {
				String[] values = (String[]) allowedValues;
				setAllowedSchemas(Arrays.stream(values).map(JsonSchema::new).toArray(size -> new JsonSchema[size]));
			} else if (allowedValues instanceof Collection) {
				setAllowedSchemas(((Collection<?>) allowedValues).stream().map(Object::toString).map(JsonSchema::new).toArray(size -> new JsonSchema[size]));
			} else if (allowedValues instanceof Object[]) {
				setAllowedSchemas(Arrays.stream(((Object[]) allowedValues)).map(Object::toString).map(JsonSchema::new).toArray(size -> new JsonSchema[size]));
			} else if (allowedValues instanceof JsonArray) {
				setAllowedSchemas(((JsonArray) allowedValues).stream().map(Object::toString).map(JsonSchema::new).toArray(size -> new JsonSchema[size]));
			}  else {
				throw new IllegalStateException("Unsupported allowed value type: " + allowedValues.getClass().getCanonicalName());
			}
		}
	}
}
