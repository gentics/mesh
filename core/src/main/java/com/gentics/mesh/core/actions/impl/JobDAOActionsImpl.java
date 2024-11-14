package com.gentics.mesh.core.actions.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.JobDAOActions;
import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * @see JobDAOActions
 */
@Singleton
public class JobDAOActionsImpl implements JobDAOActions {

	@Inject
	public JobDAOActionsImpl() {
	}

	@Override
	public HibJob loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		JobDao jobDao = ctx.tx().jobDao();
		if (perm == null) {
			return jobDao.findByUuid(uuid);
		} else {
			return jobDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibJob loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		JobDao jobDao = ctx.tx().jobDao();
		if (perm == null) {
			return jobDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public Page<? extends HibJob> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.tx().jobDao().findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibJob> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<HibJob> extraFilter) {
		return ctx.tx().jobDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public List<HibJob> createBatch(Tx tx, InternalActionContext ac, EventQueueBatch batch) {
		throw error(METHOD_NOT_ALLOWED, "Job batch creation forbidden");
	}

	@Override
	public HibJob create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.jobDao().create(ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, HibJob job, InternalActionContext ac, EventQueueBatch batch) {
		return tx.jobDao().update(job, ac, batch);
	}

	@Override
	public void delete(Tx tx, HibJob job, BulkActionContext bac) {
		tx.jobDao().delete(job, bac);
	}

	@Override
	public JobResponse transformToRestSync(Tx tx, HibJob job, InternalActionContext ac, int level, String... languageTags) {
		JobDao jobDao = tx.jobDao();
		return jobDao.transformToRestSync(job, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibJob job) {
		JobDao jobDao = tx.jobDao();
		return jobDao.getAPIPath(job, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibJob job) {
		JobDao jobDao = tx.jobDao();
		return jobDao.getETag(job, ac);
	}

	@Override
	public Page<? extends HibJob> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return ctx.tx().jobDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}
}
