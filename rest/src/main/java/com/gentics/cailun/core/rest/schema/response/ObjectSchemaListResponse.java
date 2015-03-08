package com.gentics.cailun.core.rest.schema.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.cailun.core.rest.common.response.AbstractRestListResponse;

public class ObjectSchemaListResponse extends AbstractRestListResponse {

	private List<ObjectSchemaResponse> schemas = new ArrayList<>();

	public ObjectSchemaListResponse() {
	}

	public List<ObjectSchemaResponse> getSchemas() {
		return schemas;
	}

	public void addSchema(ObjectSchemaResponse schema) {
		this.schemas.add(schema);
	}

}
