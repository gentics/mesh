package com.gentics.mesh.core.rest.branch.info;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;

/**
 * Branch / Schema assignment info REST model for lists.
 */
public class BranchInfoSchemaList implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of schema references.")
	private List<BranchSchemaInfo> schemas = new ArrayList<>();

	/**
	 * Create an empty list.
	 */
	public BranchInfoSchemaList() {
	}

	public BranchInfoSchemaList(List<BranchSchemaInfo> schemas) {
		this.schemas = schemas;
	}

	public List<BranchSchemaInfo> getSchemas() {
		return schemas;
	}

	/**
	 * Add all provided references to the list
	 * 
	 * @param references
	 * @return Fluent API
	 */
	public BranchInfoSchemaList add(SchemaReference... references) {
		for (SchemaReference reference : references) {
			getSchemas().add(new BranchSchemaInfo(reference));
		}
		return this;
	}

}
