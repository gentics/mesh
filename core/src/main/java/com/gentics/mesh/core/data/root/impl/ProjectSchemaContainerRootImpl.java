package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Project specific implementation of schema container root
 */
public class ProjectSchemaContainerRootImpl extends SchemaContainerRootImpl {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ProjectSchemaContainerRootImpl.class, MeshVertexImpl.class);
	}

	@Override
	public void addSchemaContainer(HibUser user, HibSchema schema, EventQueueBatch batch) {
		ProjectDaoWrapper projectDao = Tx.get().projectDao();
		BranchDaoWrapper branchDao = Tx.get().branchDao();

		HibProject project = getProject();
		batch.add(projectDao.onSchemaAssignEvent(project,schema, ASSIGNED));
		super.addSchemaContainer(user, schema, batch);

		// assign the latest schema version to all branches of the project
		for (HibBranch branch : branchDao.findAll(project)) {
			branch.assignSchemaVersion(user, schema.getLatestVersion(), batch);
		}
	}

	@Override
	public void removeSchemaContainer(HibSchema schema, EventQueueBatch batch) {
		ProjectDaoWrapper projectDao = Tx.get().projectDao();
		BranchDaoWrapper branchDao = Tx.get().branchDao();

		HibProject project = getProject();
		batch.add(projectDao.onSchemaAssignEvent(project, schema, UNASSIGNED));
		super.removeSchemaContainer(schema, batch);

		// unassign the schema from all branches
		for (HibBranch branch : branchDao.findAll(project)) {
			branch.unassignSchema(schema);
		}
	}
}
