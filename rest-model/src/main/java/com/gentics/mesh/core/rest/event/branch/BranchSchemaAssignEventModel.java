package com.gentics.mesh.core.rest.event.branch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.AbstractProjectEventModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public class BranchSchemaAssignEventModel extends AbstractProjectEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the branch.")
	private BranchReference branch;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the schema that was assigned.")
	private SchemaReference schema;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Status of the migration job that has been created when assigning the schema.")
	private MigrationStatus status;

	public BranchSchemaAssignEventModel() {
	}

	public BranchReference getBranch() {
		return branch;
	}

	public void setBranch(BranchReference branch) {
		this.branch = branch;
	}

	public SchemaReference getSchema() {
		return schema;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	public void setStatus(MigrationStatus status) {
		this.status = status;
	}

	public MigrationStatus getStatus() {
		return status;
	}

}
