package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.rest.branch.BranchResponse;

/**
 * DAO to access {@link HibBranch}
 */
public interface BranchDaoWrapper extends BranchDao, DaoTransformable<HibBranch, BranchResponse>, DaoWrapper<HibBranch> {
}
