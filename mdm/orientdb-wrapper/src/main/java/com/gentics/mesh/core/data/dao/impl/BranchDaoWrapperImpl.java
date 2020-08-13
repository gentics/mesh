package com.gentics.mesh.core.data.dao.impl;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
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
	public BranchResponse transformToRestSync(HibBranch branch, InternalActionContext ac, int level, String... languageTags) {
		return branch.getRoot().transformToRestSync(branch.toBranch(), ac, level, languageTags);
	}

	public HibBranch findByUuid(HibProject project, String uuid) {
		Objects.requireNonNull(project);
		HibBranch branch = project.getBranchRoot().findByUuid(uuid);
		return branch;
	}

	@Override
	public TraversalResult<? extends HibBranch> findAll(HibProject project) {
		Objects.requireNonNull(project);
		return project.getBranchRoot().findAll();
	}

	@Override
	public HibBranch loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, GraphPermission perm) {
		return project.getBranchRoot().loadObjectByUuid(ac, uuid, perm);
	}

	@Override
	public String getETag(HibBranch branch, InternalActionContext ac) {
		return branch.toBranch().getETag(ac);
	}

}
