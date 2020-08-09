package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.madl.traversal.TraversalResult;

// TODO move the contents of this to BranchDao once migration is done
public interface BranchDaoWrapper extends BranchDao, DaoTransformable<Branch, BranchResponse> {

	TraversalResult<? extends Branch> findAll(Project project);

	Branch loadObjectByUuid(Project project, InternalActionContext ac, String uuid, GraphPermission perm);

	/**
	 * Find the branch of the project by uuid.
	 * 
	 * @param project
	 * @param uuid
	 */
	Branch findByUuid(Project project, String uuid);

}
