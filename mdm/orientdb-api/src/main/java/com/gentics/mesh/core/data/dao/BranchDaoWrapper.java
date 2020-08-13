package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.madl.traversal.TraversalResult;

// TODO move the contents of this to BranchDao once migration is done
public interface BranchDaoWrapper extends BranchDao, DaoTransformable<HibBranch, BranchResponse> {

	TraversalResult<? extends HibBranch> findAll(HibProject project);

	HibBranch loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, GraphPermission perm);

	/**
	 * Find the branch of the project by uuid.
	 * 
	 * @param project
	 * @param uuid
	 */
	HibBranch findByUuid(HibProject project, String uuid);

	String getETag(HibBranch branch, InternalActionContext ac);
}
