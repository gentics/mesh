package com.gentics.mesh.core.endpoint.admin;

import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.verticle.handler.DAOActions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public class JobCrudActions implements DAOActions<Job, JobResponse> {

	@Override
	public Job load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return tx.data().jobDao().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public TransformablePage<? extends Job> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return tx.data().jobDao().findAll(ac, pagingInfo);
	}

	@Override
	public TransformablePage<? extends Job> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo, Predicate<Job> extraFilter) {
		return tx.data().jobDao().findAll(ac, pagingInfo, extraFilter);
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

}
