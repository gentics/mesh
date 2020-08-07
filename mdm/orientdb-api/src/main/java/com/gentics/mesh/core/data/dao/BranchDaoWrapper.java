package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.madl.traversal.TraversalResult;

// TODO move the contents of this to BranchDao once migration is done
public interface BranchDaoWrapper extends BranchDao, BranchRoot {

	TraversalResult<? extends Branch> findAll(Project project);

}
