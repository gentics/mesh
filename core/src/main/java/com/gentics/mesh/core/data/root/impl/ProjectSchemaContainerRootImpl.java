package com.gentics.mesh.core.data.root.impl;

import java.util.List;

import com.gentics.mesh.core.data.Release;
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
	public void addSchemaContainer(SchemaContainer schema) {
		super.addSchemaContainer(schema);

		// assign the latest schema version to all releases of the project
		List<? extends Release> releases = getProject().getReleaseRoot().findAll();
		for (Release release : releases) {
			release.assignSchemaVersion(schema.getLatestVersion());
		}
	}

	@Override
	public void removeSchemaContainer(SchemaContainer schemaContainer) {
		super.removeSchemaContainer(schemaContainer);

		// unassign the schema from all releases
		List<? extends Release> releases = getProject().getReleaseRoot().findAll();
		for (Release release : releases) {
			release.unassignSchema(schemaContainer);
		}
	}
}
