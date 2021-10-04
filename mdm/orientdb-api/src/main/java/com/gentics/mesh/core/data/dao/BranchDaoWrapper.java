package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;

/**
 * DAO to access {@link HibBranch}
 */
public interface BranchDaoWrapper extends BranchDao, OrientDBRootDao<HibProject, HibBranch> {
}
