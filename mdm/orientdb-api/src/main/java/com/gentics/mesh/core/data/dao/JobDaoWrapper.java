package com.gentics.mesh.core.data.dao;

import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public interface JobDaoWrapper extends JobDao, DaoTransformable<HibJob, JobResponse>{

	Result<? extends HibJob> findAll();

	Page<? extends HibJob> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	Page<? extends HibJob> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibJob> extraFilter);

	HibJob findByUuid(String uuid);

	HibJob findByName(String name);

	HibJob loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	HibJob loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound);

	HibJob enqueueMicroschemaMigration(HibUser creator, HibBranch branch, HibMicroschemaVersion fromVersion, HibMicroschemaVersion toVersion);

	HibJob enqueueBranchMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion, HibSchemaVersion toVersion);

	HibJob enqueueSchemaMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion, HibSchemaVersion toVersion);

	long computeCount();

	HibJob enqueueBranchMigration(HibUser creator, HibBranch branch);

	String getETag(HibJob job, InternalActionContext ac);

	String getAPIPath(HibJob job, InternalActionContext ac);

	JobResponse transformToRestSync(HibJob job, InternalActionContext ac, int level, String... languageTags);

	boolean update(HibJob job, InternalActionContext ac, EventQueueBatch batch);

	void delete(HibJob job, BulkActionContext bac);

	HibJob create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	void clear();

}
