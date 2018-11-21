package com.gentics.mesh.core.data.root.impl;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Project specific implementation of schema container root
 */
public class ProjectSchemaContainerRootImpl extends SchemaContainerRootImpl {

	public static void init(Database database) {
		database.addVertexType(ProjectSchemaContainerRootImpl.class, MeshVertexImpl.class);
	}

	@Override
	public void addSchemaContainer(User user, SchemaContainer schema) {
		super.addSchemaContainer(user, schema);

		// assign the latest schema version to all branches of the project
		for (Branch branch : getProject().getBranchRoot().findAll()) {
			branch.assignSchemaVersion(user, schema.getLatestVersion());
		}
	}

	@Override
	public void removeSchemaContainer(SchemaContainer schemaContainer) {
		super.removeSchemaContainer(schemaContainer);

		// unassign the schema from all branches
		for (Branch branch : getProject().getBranchRoot().findAll()) {
			branch.unassignSchema(schemaContainer);
		}
	}
}
