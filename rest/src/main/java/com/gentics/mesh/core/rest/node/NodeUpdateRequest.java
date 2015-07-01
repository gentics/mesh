package com.gentics.mesh.core.rest.node;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.AbstractRestModel;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public class NodeUpdateRequest extends AbstractRestModel {

	private SchemaReference schema;

	private String language;

	private Map<String, Field> fields = new HashMap<>();

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
