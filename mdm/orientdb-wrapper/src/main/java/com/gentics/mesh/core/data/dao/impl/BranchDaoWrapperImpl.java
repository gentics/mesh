package com.gentics.mesh.core.data.dao.impl;

import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.impl.BranchWrapper;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.madl.traversal.TraversalResult;

import dagger.Lazy;

@Singleton
public class BranchDaoWrapperImpl extends AbstractDaoWrapper implements BranchDaoWrapper {

	@Inject
	public BranchDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions) {
		super(boot, permissions);
	}

	// New methods
	public BranchResponse transformToRestSync(Branch branch, InternalActionContext ac, int level, String... languageTags) {
		return branch.getRoot().transformToRestSync(branch, ac, level, languageTags);
	}

	public BranchWrapper findByUuid(HibProject project, String uuid) {
		Objects.requireNonNull(project);
		Branch branch = project.getBranchRoot().findByUuid(uuid);
		if (branch == null) {
			return null;
		} else {
			return new BranchWrapper(branch);
		}
	}

	@Override
	public TraversalResult<? extends BranchWrapper> findAll(HibProject project) {
		Objects.requireNonNull(project);
		Stream<? extends Branch> nativeStream = project.getBranchRoot().findAll().stream();
		return new TraversalResult<>(nativeStream.map(BranchWrapper::new));
	}

	@Override
	public BranchWrapper loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, GraphPermission perm) {
		Branch branch = project.getBranchRoot().loadObjectByUuid(ac, uuid, perm);
		if (branch == null) {
			return null;
		} else {
			return new BranchWrapper(branch);
		}
	}

	private BranchWrapper wrap(Branch branch) {
		if (branch == null) {
			return null;
		} else {
			return new BranchWrapper(branch);
		}
	}

}
