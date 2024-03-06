package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.util.HibClassConverter;

/**
 * DAO to access {@link HibBranch}
 */
public interface BranchDaoWrapper extends PersistingBranchDao {

	@Override
	default void onRootDeleted(HibProject root, BulkActionContext bac) {
		HibClassConverter.toGraph(root).getBranchRoot().delete(bac);
	}
}
