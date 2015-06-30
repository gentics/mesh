package com.gentics.mesh.core.rest.schema.request;

import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.schema.Schema;

public class SchemaCreateRequest extends AbstractRestModel {

	private Schema schema;

	public SchemaCreateRequest() {
	}

	public Schema getSchema() {
		return schema;
	}

	public void setSchema(Schema schema) {
		this.schema = schema;
	}

}
