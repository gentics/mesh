package com.gentics.mesh.core.data.dao.impl;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.madl.traversal.TraversalResult;

@Singleton
public class BranchDaoWrapperImpl implements BranchDaoWrapper {

	@Inject
	public BranchDaoWrapperImpl() {
	}

	// New methods

	public BranchResponse transformToRestSync(Branch branch, InternalActionContext ac, int level, String... languageTags) {
		return branch.getRoot().transformToRestSync(branch, ac, level, languageTags);
	}

	public Branch findByUuid(Project project, String uuid) {
		Objects.requireNonNull(project);
		return project.getBranchRoot().findByUuid(uuid);
	}

	@Override
	public TraversalResult<? extends Branch> findAll(Project project) {
		Objects.requireNonNull(project);
		return project.getBranchRoot().findAll();
	}

	@Override
	public Branch loadObjectByUuid(Project project, InternalActionContext ac, String uuid, GraphPermission perm) {
		return project.getBranchRoot().loadObjectByUuid(ac, uuid, perm);
	}

}
