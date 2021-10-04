package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractRootDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

/**
 * @see BranchDaoWrapper
 */
@Singleton
public class BranchDaoWrapperImpl extends AbstractRootDaoWrapper<BranchResponse, HibBranch, Branch, HibProject> implements BranchDaoWrapper {

	private Lazy<Database> db;

	@Inject
	public BranchDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions, Lazy<Database> db) {
		super(boot, permissions);
		// TODO Fix assignment - Inject DB
		this.db = db;
	}

	/**
	 * Transform the given branch to REST.
	 * 
	 * @param branch
	 * @param ac
	 * @param level
	 * @param languageTags
	 * @return
	 */
	public BranchResponse transformToRestSync(HibBranch branch, InternalActionContext ac, int level, String... languageTags) {
		Branch graphBranch = toGraph(branch);
		return graphBranch.getRoot().transformToRestSync(graphBranch, ac, level, languageTags);
	}

	/**
	 * Load the branch from the project using the provided uuid.
	 * 
	 * @param project
	 * @param uuid
	 * @return Loaded branch or null when the branch can't be found
	 */
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

// TODO remove if unneeded
//	@Override
//	public long globalCount() {
//		return db.get().count(Branch.class);
//	}

	@Override
	public Stream<? extends HibBranch> findAllStream(HibProject root, InternalActionContext ac,
			InternalPermission permission) {
		return toGraph(root).getBranchRoot().findAllStream(ac, permission);
	}

	@Override
	public Result<? extends HibBranch> findAllDynamic(HibProject root) {
		return toGraph(root).getBranchRoot().findAllDynamic();
	}

	@Override
	public Page<? extends HibBranch> findAllNoPerm(HibProject root, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return toGraph(root).getBranchRoot().findAllNoPerm(ac, pagingInfo);
	}

	@Override
	public HibBranch findByName(HibProject root, InternalActionContext ac, String name, InternalPermission perm) {
		return toGraph(root).getBranchRoot().findByName(ac, name, perm);
	}

	@Override
	public HibBranch checkPerms(HibProject root, HibBranch element, String uuid, InternalActionContext ac,
			InternalPermission perm, boolean errorIfNotFound) {
		return toGraph(root).getBranchRoot().checkPerms(toGraph(element), uuid, ac, perm, errorIfNotFound);
	}

	@Override
	public void addItem(HibProject root, HibBranch item) {
		toGraph(root).getBranchRoot().addItem(toGraph(item));
	}

	@Override
	public void removeItem(HibProject root, HibBranch item) {
		toGraph(root).getBranchRoot().removeItem(toGraph(item));
	}

	@Override
	public String getRootLabel(HibProject root) {
		return toGraph(root).getBranchRoot().getRootLabel();
	}

	@Override
	public Class<? extends HibBranch> getPersistenceClass(HibProject root) {
		return toGraph(root).getBranchRoot().getPersistanceClass();
	}

	@Override
	public long globalCount(HibProject root) {
		return toGraph(root).getBranchRoot().globalCount();
	}

	@Override
	public PermissionInfo getRolePermissions(HibProject root, HibBaseElement element, InternalActionContext ac,
			String roleUuid) {
		return toGraph(root).getBranchRoot().getRolePermissions(element, ac, roleUuid);
	}

	@Override
	public Result<? extends HibRole> getRolesWithPerm(HibProject root, HibBaseElement vertex, InternalPermission perm) {
		return toGraph(root).getBranchRoot().getRolesWithPerm(vertex, perm);
	}

	@Override
	public void delete(HibProject root, HibBranch element, BulkActionContext bac) {
		toGraph(root).getBranchRoot().delete(toGraph(element), bac);
	}

	@Override
	public boolean update(HibProject root, HibBranch element, InternalActionContext ac, EventQueueBatch batch) {
		return toGraph(root).getBranchRoot().update(toGraph(element), ac, batch);
	}

	@Override
	protected RootVertex<Branch> getRoot(HibProject root) {
		return toGraph(root).getBranchRoot();
	}
}
