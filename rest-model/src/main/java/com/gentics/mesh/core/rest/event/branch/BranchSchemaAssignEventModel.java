package com.gentics.mesh.core.rest.event.branch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public class BranchSchemaAssignEventModel extends AbstractBranchAssignEventModel<SchemaReference> {

	@JsonCreator
	public BranchSchemaAssignEventModel(String origin, EventCauseInfo cause, MeshEvent event, ProjectReference project, BranchReference branch,
		SchemaReference schema, MigrationStatus status) {
		super(origin, cause, event, project, branch, schema, status);
	}

}
