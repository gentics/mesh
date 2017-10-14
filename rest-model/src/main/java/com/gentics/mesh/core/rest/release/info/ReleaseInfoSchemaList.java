package com.gentics.mesh.core.rest.release.info;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public class ReleaseInfoSchemaList implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of schema references.")
	private List<ReleaseSchemaInfo> schemas = new ArrayList<>();

	/**
	 * Create an empty list.
	 */
	public ReleaseInfoSchemaList() {
	}

	public List<ReleaseSchemaInfo> getSchemas() {
		return schemas;
	}

	/**
	 * Add all provided references to the list
	 * 
	 * @param references
	 * @return Fluent API
	 */
	public ReleaseInfoSchemaList add(SchemaReference... references) {
		for (SchemaReference reference : references) {
			getSchemas().add(new ReleaseSchemaInfo(reference));
		}
		return this;
	}

}
