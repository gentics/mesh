package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractCoreDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;
import io.reactivex.Completable;

/**
 * DAO for jobs.
 */
@Singleton
public class JobDaoWrapperImpl extends AbstractCoreDaoWrapper<JobResponse, HibJob, Job> implements JobDaoWrapper {

	@Inject
	public JobDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot) {
		super(boot);
	}

	@Override
	public Result<? extends HibJob> findAll() {
		return boot.get().meshRoot().getJobRoot().findAll();
	}

	@Override
	public Page<? extends HibJob> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().meshRoot().getJobRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibJob> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibJob> extraFilter) {
		return boot.get().meshRoot().getJobRoot().findAll(ac, pagingInfo, job -> {
			return extraFilter.test(job);
		});
	}

	@Override
	public HibJob findByName(String name) {
		return boot.get().meshRoot().getJobRoot().findByName(name);
	}

	@Override
	public HibJob enqueueSchemaMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion, HibSchemaVersion toVersion) {
		return boot.get().meshRoot().getJobRoot().enqueueSchemaMigration(creator, branch, fromVersion, toVersion);
	}

	@Override
	public HibJob enqueueBranchMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion, HibSchemaVersion toVersion) {
		return boot.get().meshRoot().getJobRoot().enqueueBranchMigration(creator, branch, fromVersion, toVersion);
	}

	@Override
	public HibJob enqueueMicroschemaMigration(HibUser creator, HibBranch branch, HibMicroschemaVersion fromVersion,
		HibMicroschemaVersion toVersion) {
		return boot.get().meshRoot().getJobRoot().enqueueMicroschemaMigration(creator, branch, fromVersion, toVersion);
	}

	@Override
	public HibJob enqueueBranchMigration(HibUser creator, HibBranch branch) {
		return boot.get().meshRoot().getJobRoot().enqueueBranchMigration(creator, branch);
	}

	/**
	 * Apply the permissions to the job.
	 * 
	 * @param batch
	 * @param role
	 * @param recursive
	 * @param permissionsToGrant
	 * @param permissionsToRevoke
	 */
	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
		Role graphRole = toGraph(role);
		boot.get().meshRoot().getJobRoot().applyPermissions(batch, graphRole, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public boolean update(HibJob job, InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().meshRoot().getJobRoot().update(toGraph(job), ac, batch);
	}

	@Override
	public HibJob create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().meshRoot().getJobRoot().create(ac, batch, uuid);
	}

	@Override
	public void delete(HibJob job, BulkActionContext bac) {
		boot.get().meshRoot().getJobRoot().delete(toGraph(job), bac);
	}

	@Override
	public void clear() {
		boot.get().meshRoot().getJobRoot().clear();
	}

	@Override
	public Completable process() {
		JobRoot jobRoot = boot.get().meshRoot().getJobRoot();
		return jobRoot.process();
	}

	@Override
	public long count() {
		return boot.get().meshRoot().getJobRoot().computeCount();
	}

	@Override
	public HibJob findByUuid(String uuid) {
		return boot.get().meshRoot().getJobRoot().findByUuid(uuid);
	}

	@Override
	public Page<? extends HibJob> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().meshRoot().getJobRoot().findAllNoPerm(ac, pagingInfo);
	}

	@Override
	public HibJob enqueueVersionPurge(HibUser user, HibProject project, ZonedDateTime before) {
		return boot.get().meshRoot().getJobRoot().enqueueVersionPurge(user, project, before);
	}

	@Override
	public HibJob enqueueVersionPurge(HibUser user, HibProject project) {
		return boot.get().meshRoot().getJobRoot().enqueueVersionPurge(user, project);
	}

	@Override
	public void purgeFailed() {
		boot.get().meshRoot().getJobRoot().purgeFailed();
	}

	@Override
	protected RootVertex<Job> getRoot() {
		return boot.get().meshRoot().getJobRoot();
	}
}
