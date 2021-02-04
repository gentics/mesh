package com.gentics.mesh.core.data.dao.impl;

import com.gentics.mesh.cli.ODBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.AbstractODBDaoWrapper;
import com.gentics.mesh.core.data.dao.OrientDBJobDao;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import dagger.Lazy;
import io.reactivex.Completable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.function.Predicate;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

@Singleton
public class ODBJobDaoWrapperImpl extends AbstractODBDaoWrapper<HibJob> implements OrientDBJobDao {

	@Inject
	public ODBJobDaoWrapperImpl(Lazy<ODBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
	}

	@Override
	public Result<? extends HibJob> findAll() {
		return boot.get().jobRoot().findAll();
	}

	@Override
	public HibJob findByUuid(String uuid) {
		return boot.get().jobRoot().findByUuid(uuid);
	}

	@Override
	public Page<? extends HibJob> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().jobRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibJob> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibJob> extraFilter) {
		return boot.get().jobRoot().findAll(ac, pagingInfo, job -> {
			return extraFilter.test(job);
		});
	}

	@Override
	public HibJob findByName(String name) {
		return boot.get().jobRoot().findByName(name);
	}

	@Override
	public HibJob loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		return boot.get().jobRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public HibJob loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().jobRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	@Override
	public HibJob enqueueSchemaMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion, HibSchemaVersion toVersion) {
		return boot.get().jobRoot().enqueueSchemaMigration(creator, branch, fromVersion, toVersion);
	}

	@Override
	public HibJob enqueueBranchMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion, HibSchemaVersion toVersion) {
		return boot.get().jobRoot().enqueueBranchMigration(creator, branch, fromVersion, toVersion);
	}

	@Override
	public HibJob enqueueMicroschemaMigration(HibUser creator, HibBranch branch, HibMicroschemaVersion fromVersion,
		HibMicroschemaVersion toVersion) {
		return boot.get().jobRoot().enqueueMicroschemaMigration(creator, branch, fromVersion, toVersion);
	}

	@Override
	public HibJob enqueueBranchMigration(HibUser creator, HibBranch branch) {
		return boot.get().jobRoot().enqueueBranchMigration(creator, branch);
	}

	@Override
	public HibJob enqueueVersionPurge(HibUser creator, HibProject project, ZonedDateTime before) {
		return boot.get().jobRoot().enqueueVersionPurge(creator, project, before);
	}

	@Override
	public HibJob enqueueVersionPurge(HibUser creator, HibProject project) {
		return boot.get().jobRoot().enqueueVersionPurge(creator, project);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
		Role graphRole = toGraph(role);
		boot.get().jobRoot().applyPermissions(batch, graphRole, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public long computeCount() {
		return boot.get().jobRoot().computeCount();
	}

	@Override
	public String getAPIPath(HibJob job, InternalActionContext ac) {
		return toGraph(job).getAPIPath(ac);
	}

	@Override
	public String getETag(HibJob job, InternalActionContext ac) {
		return toGraph(job).getETag(ac);
	}

	@Override
	public boolean update(HibJob job, InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().jobRoot().update(toGraph(job), ac, batch);
	}

	@Override
	public HibJob create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().jobRoot().create(ac, batch, uuid);
	}

	@Override
	public void delete(HibJob job, BulkActionContext bac) {
		boot.get().jobRoot().delete(toGraph(job), bac);
	}

	@Override
	public JobResponse transformToRestSync(HibJob job, InternalActionContext ac, int level, String... languageTags) {
		return toGraph(job).transformToRestSync(ac, level, languageTags);
	}

	@Override
	public void clear() {
		boot.get().jobRoot().clear();
	}

	@Override
	public Completable process() {
		JobRoot jobRoot = boot.get().jobRoot();
		return jobRoot.process();
	}

	@Override
	public long globalCount() {
		return boot.get().jobRoot().computeCount();
	}

	@Override
	public HibJob findByUuidGlobal(String uuid) {
		return boot.get().jobRoot().findByUuid(uuid);
	}

}
