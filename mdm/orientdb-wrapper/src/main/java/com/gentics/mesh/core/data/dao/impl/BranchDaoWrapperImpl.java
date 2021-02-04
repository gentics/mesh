package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Objects;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.OrientDBBranchDao;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

@Singleton
public class BranchDaoWrapperImpl extends AbstractDaoWrapper<HibBranch> implements OrientDBBranchDao {

	private Database db;

	@Inject
	public BranchDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
		this.db = db;
	}

	// New methods
	public BranchResponse transformToRestSync(HibBranch branch, InternalActionContext ac, int level, String... languageTags) {
		Branch graphBranch = toGraph(branch);
		return graphBranch.getRoot().transformToRestSync(graphBranch, ac, level, languageTags);
	}

	public HibBranch findByUuid(HibProject project, String uuid) {
		Objects.requireNonNull(project);
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().findByUuid(uuid);
	}

	@Override
	public HibBranch findByName(HibProject project, String name) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().findByName(name);
	}

	@Override
	public Result<? extends HibBranch> findAll(HibProject project) {
		Objects.requireNonNull(project);
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().findAll();
	}

	@Override
	public Page<? extends HibBranch> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<HibBranch> extraFilter) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().findAll(ac, pagingInfo, branch -> {
			return extraFilter.test(branch);
		});
	}

	@Override
	public boolean update(HibBranch branch, InternalActionContext ac, EventQueueBatch batch) {
		Branch graphBranch = toGraph(branch);
		return graphBranch.update(ac, batch);
	}

	@Override
	public HibBranch loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().loadObjectByUuid(ac, uuid, perm);
	}

	@Override
	public HibBranch loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public String getETag(HibBranch branch, InternalActionContext ac) {
		Branch graphBranch = toGraph(branch);
		return graphBranch.getETag(ac);
	}

	@Override
	public String getAPIPath(HibBranch branch, InternalActionContext ac) {
		Branch graphBranch = toGraph(branch);
		return graphBranch.getAPIPath(ac);
	}

	@Override
	public Page<? extends HibBranch> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().findAll(ac, pagingInfo);
	}

	@Override
	public HibBranch create(HibProject project, String name, HibUser user, EventQueueBatch batch) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().create(name, user, batch);
	}

	@Override
	public HibBranch create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().create(ac, batch, uuid);
	}

	@Override
	public HibBranch create(HibProject project, String name, HibUser creator, String uuid, boolean setLatest, HibBranch baseBranch,
		EventQueueBatch batch) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().create(name, creator, uuid, setLatest, baseBranch, batch);
	}

	@Override
	public HibBranch getLatestBranch(HibProject project) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().getLatestBranch();
	}

	@Override
	public HibBranch findByUuidGlobal(String uuid) {
		throw new NotImplementedException("Not supported");
	}

	@Override
	public long globalCount() {
		return db.count(Branch.class);
	}

}
