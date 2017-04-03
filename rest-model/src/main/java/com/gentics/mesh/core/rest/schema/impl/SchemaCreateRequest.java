package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import java.util.List;

public class SchemaCreateRequest extends SchemaUpdateRequest {
	@Override
	public SchemaCreateRequest setVersion(int version) {
		super.setVersion(version);
		return this;
	}

	@Override
	public SchemaCreateRequest setDescription(String description) {
		super.setDescription(description);
		return this;
	}

	@Override
	public SchemaCreateRequest setName(String name) {
		super.setName(name);
		return this;
	}

	@Override
	public SchemaCreateRequest setSegmentField(String segmentField) {
		super.setSegmentField(segmentField);
		return this;
	}

	@Override
	public SchemaCreateRequest setFields(List<FieldSchema> fields) {
		super.setFields(fields);
		return this;
	}

	@Override
	public SchemaCreateRequest setContainer(boolean flag) {
		super.setContainer(flag);
		return this;
	}

	@Override
	public SchemaCreateRequest addField(FieldSchema fieldSchema) {
		super.addField(fieldSchema);
		return this;
	}

	@Override
	public SchemaCreateRequest setDisplayField(String displayField) {
		super.setDisplayField(displayField);
		return this;
	}

	@Override
	public SchemaCreateRequest addField(FieldSchema fieldSchema, String afterFieldName) {
		super.addField(fieldSchema, afterFieldName);
		return this;
	}
}
