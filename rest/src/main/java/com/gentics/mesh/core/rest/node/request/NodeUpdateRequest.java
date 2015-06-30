package com.gentics.mesh.core.rest.node.request;

import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.schema.response.SchemaReference;

public class NodeUpdateRequest extends AbstractRestModel {

	private SchemaReference schema;

	private String language;

	// private NodeFieldContainer fields;

	public NodeUpdateRequest() {
	}

	public SchemaReference getSchema() {
		return schema;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	// public NodeFieldContainer getFields() {
	// return fields;
	// }

	// public void setFields(NodeFieldContainer fields) {
	// this.fields = fields;
	// }

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

}
