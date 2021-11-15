package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ALLOW_KEY;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;

import io.vertx.core.json.JsonArray;

/**
 * @see StringFieldSchema
 */
public class StringFieldSchemaImpl extends AbstractFieldSchema implements StringFieldSchema {

	@JsonProperty("allow")
	@JsonPropertyDescription("List of allowed string values. Only one of the listed values will be accepted during update and create requests.")
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
	public Map<String, Object> getAllChangeProperties() {
		Map<String, Object> map = super.getAllChangeProperties();
		map.put(ALLOW_KEY, getAllowedValues());
		return map;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		Object allowedValues = fieldProperties.get(ALLOW_KEY);
		if (allowedValues != null) {
			if (allowedValues instanceof String[]) {
				setAllowedValues((String[]) allowedValues);
			} else if (allowedValues instanceof Collection) {
				setAllowedValues(((Collection<?>) allowedValues).stream().map(Object::toString).toArray(size -> new String[size]));
			} else if (allowedValues instanceof Object[]) {
				setAllowedValues(Arrays.stream(((Object[]) allowedValues)).map(Object::toString).toArray(size -> new String[size]));
			} else if (allowedValues instanceof JsonArray) {
				setAllowedValues(((JsonArray) allowedValues).stream().map(Object::toString).toArray(size -> new String[size]));
			}  else {
				throw new IllegalStateException("Unsupported allowed value type: " + allowedValues.getClass().getCanonicalName());
			}
		}
	}

	@Override
	public String toString() {
		return "name:" + getName() + ",label:" + getLabel() + ",type:" + getType();
	}
}
