package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;

/**
 * A persisting extension to {@link BranchDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingBranchDao extends BranchDao, PersistingRootDao<HibProject, HibBranch> {

}
