package com.gentics.mesh.core.endpoint.admin;

import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.DAOActions;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public class JobCrudActions implements DAOActions<Job, JobResponse> {

	@Override
	public Job loadByUuid(DAOActionContext ctx, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		JobDaoWrapper jobDao = ctx.tx().data().jobDao();
		if (perm == null) {
			return jobDao.findByUuid(uuid);
		} else {
			return jobDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public Job loadByName(DAOActionContext ctx, String name, GraphPermission perm, boolean errorIfNotFound) {
		JobDaoWrapper jobDao = ctx.tx().data().jobDao();
		if (perm == null) {
			return jobDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public TransformablePage<? extends Job> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.tx().data().jobDao().findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends Job> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<Job> extraFilter) {
		return ctx.tx().data().jobDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public Job create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.data().jobDao().create(ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, Job element, InternalActionContext ac, EventQueueBatch batch) {
		return tx.data().jobDao().update(element, ac, batch);
	}

	@Override
	public void delete(Tx tx, Job element, BulkActionContext bac) {
		tx.data().jobDao().delete(element, bac);
	}

	@Override
	public JobResponse transformToRestSync(Tx tx, Job job, InternalActionContext ac, int level, String... languageTags) {
		// return tx.data().jobDao();
		return job.transformToRestSync(ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, Job job) {
		// JobDao jobDao = tx.data().jobDao();
		return job.getAPIPath(ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, Job job) {
		return job.getETag(ac);
	}

}
