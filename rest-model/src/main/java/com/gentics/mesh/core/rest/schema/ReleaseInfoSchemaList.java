package com.gentics.mesh.core.rest.schema;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;

public class ReleaseInfoSchemaList implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of schema references.")
	@JsonDeserialize(contentAs = SchemaReferenceImpl.class)
	private List<SchemaReference> schemas = new ArrayList<>();

	/**
	 * Create an empty list.
	 */
	public ReleaseInfoSchemaList() {
	}

	public List<SchemaReference> getSchemas() {
		return schemas;
	}

}
