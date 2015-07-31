package com.gentics.mesh.core.rest.node;

import java.util.Map;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public class NodeUpdateRequest implements RestModel {

	private SchemaReference schema;

	private String language;

	private FieldMap fields = new FieldMapImpl();

	public NodeUpdateRequest() {
	}

	public SchemaReference getSchema() {
		return schema;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public Map<String, Field> getFields() {
		return fields;
	}

}
