package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

/**
 * Abstract class for field container REST Model POJO's (e.g: {@link SchemaModel}, {@link MicroschemaModel}
 */
public abstract class AbstractFieldSchemaContainer extends AbstractGenericRestResponse implements FieldSchemaContainer {

	@JsonPropertyDescription("Version of the schema")
	private int version;

	@JsonPropertyDescription("Description of the schema")
	private String description;

	@JsonPropertyDescription("Name of the schema")
	private String name;
	
	@JsonPropertyDescription("List of schema fields")
	private List<FieldSchema> fields = new ArrayList<>();

	public AbstractFieldSchemaContainer(String name) {
		this.name = name;
	}

	public AbstractFieldSchemaContainer() {
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Optional<FieldSchema> getFieldSchema(String fieldName) {
		return fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst();
	}

	@Override
	public FieldSchema getField(String fieldName) {
		return (FieldSchema) fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
	}

	@Override
	public <T> T getField(String fieldName, Class<T> classOfT) {
		return (T) fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}

	@Override
	public List<FieldSchema> getFields() {
		return fields;
	}

	@Override
	@JsonIgnore
	public Map<String, FieldSchema> getFieldsAsMap() {
		Map<String, FieldSchema> map = new HashMap<>();
		for (FieldSchema field : getFields()) {
			map.put(field.getName(), field);
		}
		return map;
	}

	@Override
	public void removeField(String name) {
		if (name == null) {
			return;
		}
		fields.removeIf(field -> name.equals(field.getName()));
	}

	@Override
	public void addField(FieldSchema fieldSchema) {
		Objects.requireNonNull(fieldSchema, "The field schema must not be null");
		Objects.requireNonNull(fieldSchema.getName(), "The field schema must have a valid name");
		this.fields.add(fieldSchema);
	}

	@Override
	public void addField(FieldSchema fieldSchema, String afterFieldName) {
		int index = fields.size();
		if (afterFieldName != null) {
			for (int i = 0; i < fields.size(); i++) {
				if (afterFieldName.equals(fields.get(i).getName())) {
					index = i;
					break;
				}
			}
		}
		if (index < fields.size()) {
			index = index + 1;
		}
		this.fields.add(index, fieldSchema);
	}

	@Override
	public void setFields(List<FieldSchema> fields) {
		this.fields = fields;
	}

	@Override
	public void validate() {
		if (StringUtils.isEmpty(getName())) {
			throw error(BAD_REQUEST, "schema_error_no_name");
		}

		Set<String> fieldLabels = new HashSet<>();
		Set<String> fieldNames = new HashSet<>();

		for (FieldSchema field : getFields()) {
			if (field.getName() != null) {
				if (!fieldNames.add(field.getName())) {
					throw error(BAD_REQUEST, "schema_error_duplicate_field_name", field.getName());
				}
			}

			if (field.getLabel() != null) {
				if (!fieldLabels.add(field.getLabel())) {
					throw error(BAD_REQUEST, "schema_error_duplicate_field_label", field.getName(), field.getLabel());
				}
			}
			field.validate();
		}
	}

	@Override
	public void assertForUnhandledFields(FieldMap fieldMap) {
		Set<String> allFieldsOfRequest = new HashSet<>(fieldMap.keySet());
		for (FieldSchema fieldSchema : getFields()) {
			allFieldsOfRequest.remove(fieldSchema.getName());
		}
		if (allFieldsOfRequest.size() > 0) {
			throw error(BAD_REQUEST, "node_unhandled_fields", getName(), Arrays.toString(allFieldsOfRequest.toArray()));
		}
	}

}
