package com.gentics.mesh.core.rest.schema.request;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.common.response.SchemaFieldDeserializer;
import com.gentics.mesh.core.rest.schema.FieldSchema;

public class SchemaCreateRequest extends AbstractRestModel {

	private String name;
	private String displayField;

	@JsonProperty("container")
	private boolean isContainer;

	@JsonProperty("binary")
	private boolean isBinary;

	@JsonDeserialize(contentUsing = SchemaFieldDeserializer.class)
	private List<FieldSchema> fields = new ArrayList<>();

	public SchemaCreateRequest() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayField() {
		return displayField;
	}

	public void setDisplayField(String displayField) {
		this.displayField = displayField;
	}

	public List<? extends FieldSchema> getFields() {
		return fields;
	}

	public boolean isBinary() {
		return isBinary;
	}

	public <T extends FieldSchema> void addField(T fieldSchema) {
		fields.add(fieldSchema);
	}

	public void setFields(List<FieldSchema> fields) {
		this.fields = fields;
	}
}
