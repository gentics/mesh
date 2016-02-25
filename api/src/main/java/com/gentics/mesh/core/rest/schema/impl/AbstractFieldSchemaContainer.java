package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

public abstract class AbstractFieldSchemaContainer implements FieldSchemaContainer {

	private String uuid;
	private String[] permissions = {};
	private String[] rolePerms;
	private int version;
	private String description;
	private String name;
	private List<FieldSchema> fields = new ArrayList<>();

	public AbstractFieldSchemaContainer(String name) {
		this.name = name;
	}

	public AbstractFieldSchemaContainer() {
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String[] getPermissions() {
		return permissions;
	}

	@Override
	public void setPermissions(String... permissions) {
		this.permissions = permissions;
	}

	@Override
	public String[] getRolePerms() {
		return rolePerms;
	}

	@Override
	public void setRolePerms(String... rolePerms) {
		this.rolePerms = rolePerms;
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
	public void removeField(String name) {
		if (name == null) {
			return;
		}
		fields.removeIf(field -> name.equals(field.getName()));
	}

	@Override
	public void addField(FieldSchema fieldSchema) {
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

}
