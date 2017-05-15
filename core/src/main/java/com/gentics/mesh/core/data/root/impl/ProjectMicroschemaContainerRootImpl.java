package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;

import java.util.List;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Project specific implementation of microschema container root.
 */
public class ProjectMicroschemaContainerRootImpl extends MicroschemaContainerRootImpl {

	public static void init(Database database) {
		database.addVertexType(ProjectMicroschemaContainerRootImpl.class, MeshVertexImpl.class);
	}

	/**
	 * Get the project.
	 * 
	 * @return project
	 */
	protected Project getProject() {
		return in(HAS_MICROSCHEMA_ROOT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public void addMicroschema(MicroschemaContainer microschema) {
		super.addMicroschema(microschema);

		// assign the latest schema version to all releases of the project
		List<? extends Release> releases = getProject().getReleaseRoot().findAll();
		for (Release release : releases) {
			release.assignMicroschemaVersion(microschema.getLatestVersion());
		}
	}

	@Override
	public void removeMicroschema(MicroschemaContainer microschema) {
		super.removeMicroschema(microschema);

		// unassign the schema from all releases
		List<? extends Release> releases = getProject().getReleaseRoot().findAll();
		for (Release release : releases) {
			release.unassignMicroschema(microschema);
		}
	}
}
