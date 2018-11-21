package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.User;
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
	public void addMicroschema(User user, MicroschemaContainer microschema) {
		super.addMicroschema(user, microschema);

		// assign the latest schema version to all branches of the project
		for (Branch branch : getProject().getBranchRoot().findAll()) {
			branch.assignMicroschemaVersion(user, microschema.getLatestVersion());
		}
	}

	@Override
	public void removeMicroschema(MicroschemaContainer microschema) {
		super.removeMicroschema(microschema);

		// unassign the schema from all branches
		for (Branch branch : getProject().getBranchRoot().findAll()) {
			branch.unassignMicroschema(microschema);
		}
	}
}
