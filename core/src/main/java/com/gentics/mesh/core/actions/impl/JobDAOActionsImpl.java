package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.JobDAOActions;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class JobDAOActionsImpl implements JobDAOActions {

	@Inject
	public JobDAOActionsImpl() {
	}

	@Override
	public HibJob loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		JobDaoWrapper jobDao = ctx.tx().data().jobDao();
		if (perm == null) {
			return jobDao.findByUuid(uuid);
		} else {
			return jobDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibJob loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		JobDaoWrapper jobDao = ctx.tx().data().jobDao();
		if (perm == null) {
			return jobDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public TransformablePage<? extends HibJob> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.tx().data().jobDao().findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibJob> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<HibJob> extraFilter) {
		return ctx.tx().data().jobDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public HibJob create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.data().jobDao().create(ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, HibJob job, InternalActionContext ac, EventQueueBatch batch) {
		return tx.data().jobDao().update(job, ac, batch);
	}

	@Override
	public void delete(Tx tx, HibJob job, BulkActionContext bac) {
		tx.data().jobDao().delete(job, bac);
	}

	@Override
	public JobResponse transformToRestSync(Tx tx, HibJob job, InternalActionContext ac, int level, String... languageTags) {
		JobDaoWrapper jobDao = tx.data().jobDao();
		return jobDao.transformToRestSync(job, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibJob job) {
		JobDaoWrapper jobDao = tx.data().jobDao();
		return jobDao.getAPIPath(job, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibJob job) {
		JobDaoWrapper jobDao = tx.data().jobDao();
		return jobDao.getETag(job, ac);
	}

}
