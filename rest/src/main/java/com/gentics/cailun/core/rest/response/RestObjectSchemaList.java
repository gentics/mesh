package com.gentics.cailun.core.rest.response;

import java.util.ArrayList;
import java.util.List;

public class RestObjectSchemaList {

	private List<RestObjectSchema> schemas = new ArrayList<>();

	public RestObjectSchemaList() {
	}

	public List<RestObjectSchema> getSchemas() {
		return schemas;
	}

	public void addSchema(RestObjectSchema schema) {
		this.schemas.add(schema);
	}

}
