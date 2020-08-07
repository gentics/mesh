package com.gentics.mesh.core.data.dao.impl;

import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.impl.BranchWrapper;
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

	public BranchWrapper findByUuid(Project project, String uuid) {
		Objects.requireNonNull(project);
		Branch branch = project.getBranchRoot().findByUuid(uuid);
		if (branch == null) {
			return null;
		} else {
			return new BranchWrapper(branch);
		}
	}

	@Override
	public TraversalResult<? extends BranchWrapper> findAll(Project project) {
		Objects.requireNonNull(project);
		Stream<? extends Branch> nativeStream = project.getBranchRoot().findAll().stream();
		return new TraversalResult<>(nativeStream.map(BranchWrapper::new));
	}

	@Override
	public BranchWrapper loadObjectByUuid(Project project, InternalActionContext ac, String uuid, GraphPermission perm) {
		Branch branch = project.getBranchRoot().loadObjectByUuid(ac, uuid, perm);
		if (branch == null) {
			return null;
		} else {
			return new BranchWrapper(branch);
		}
	}

}
