package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Project specific implementation of schema container root
 */
public class ProjectSchemaRootImpl extends SchemaRootImpl {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ProjectSchemaRootImpl.class, MeshVertexImpl.class);
	}

	@Override
	public void addSchemaContainer(User user, Schema schema, EventQueueBatch batch) {
		Project project = getProject();
		batch.add(project.onSchemaAssignEvent(schema, ASSIGNED));
		super.addSchemaContainer(user, schema, batch);

		// assign the latest schema version to all branches of the project
		for (Branch branch : project.getBranchRoot().findAll()) {
			branch.assignSchemaVersion(user, schema.getLatestVersion(), batch);
		}
	}

	@Override
	public void removeSchemaContainer(Schema schema, EventQueueBatch batch) {
		Project project = getProject();
		batch.add(project.onSchemaAssignEvent(schema, UNASSIGNED));
		super.removeSchemaContainer(schema, batch);

		// unassign the schema from all branches
		for (Branch branch : project.getBranchRoot().findAll()) {
			branch.unassignSchema(schema);
		}
	}
}
