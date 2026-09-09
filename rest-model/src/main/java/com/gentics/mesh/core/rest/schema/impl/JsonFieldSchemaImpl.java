package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ALLOW_KEY;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.JsonFieldSchema;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonArray;

/**
 * @see JsonFieldSchema
 */
public class JsonFieldSchemaImpl extends AbstractFieldSchema implements JsonFieldSchema {

	@JsonProperty("allow")
	@JsonPropertyDescription("List of allowed JSON schemas, the data of this field must match.")
	private JsonNode[] allowedSchemas;

	@Override
	public String getType() {
		return FieldTypes.JSON.toString();
	}

	@Override
	public JsonNode[] getAllowedSchemas() {
		return allowedSchemas;
	}

	@Override
	public JsonFieldSchemaImpl setAllowedSchemas(JsonNode... allowedSchemas) {
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
			if (allowedValues instanceof JsonNode[] allowedSchemas) {
				setAllowedSchemas(allowedSchemas);
			} else if (allowedValues instanceof String[] values) {
				setAllowedSchemas(Arrays.stream(values).map(JsonUtil::toJsonNode).toArray(size -> new JsonNode[size]));
			} else if (allowedValues instanceof Collection<?> values) {
				setAllowedSchemas(values.stream().map(Object::toString).map(JsonUtil::toJsonNode).toArray(size -> new JsonNode[size]));
			} else if (allowedValues instanceof Object[] values) {
				setAllowedSchemas(Arrays.stream(values).map(Object::toString).map(JsonUtil::toJsonNode).toArray(size -> new JsonNode[size]));
			} else if (allowedValues instanceof JsonArray array) {
				setAllowedSchemas(array.stream().map(Object::toString).map(JsonUtil::toJsonNode).toArray(size -> new JsonNode[size]));
			}  else {
				throw new IllegalStateException("Unsupported allowed value type: " + allowedValues.getClass().getCanonicalName());
			}
		}
	}
}
