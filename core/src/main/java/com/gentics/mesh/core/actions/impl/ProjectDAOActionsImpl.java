package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * @see ProjectDAOActions
 */
@Singleton
public class ProjectDAOActionsImpl implements ProjectDAOActions {

	@Inject
	public ProjectDAOActionsImpl() {
	}

	@Override
	public Project loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		ProjectDao projectDao = ctx.tx().projectDao();
		if (perm == null) {
			return projectDao.findByUuid(uuid);
		} else {
			return projectDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public Project loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		if (perm == null) {
			return ctx.tx().projectDao().findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public Page<? extends Project> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.tx().projectDao().findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends Project> loadAll(DAOActionContext ctx, PagingParameters pagingInfo,
		Predicate<Project> extraFilter) {
		return ctx.tx().projectDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public Project create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.projectDao().create(ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, Project element, InternalActionContext ac, EventQueueBatch batch) {
		return tx.projectDao().update(element, ac, batch);
	}

	@Override
	public void delete(Tx tx, Project project, BulkActionContext bac) {
		ProjectDao projectDao = tx.projectDao();
		projectDao.delete(project, bac);
	}

	@Override
	public ProjectResponse transformToRestSync(Tx tx, Project project, InternalActionContext ac, int level, String... languageTags) {
		ProjectDao projectDao = tx.projectDao();
		return projectDao.transformToRestSync(project, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, Project project) {
		ProjectDao projectDao = tx.projectDao();
		return projectDao.getAPIPath(project, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, Project project) {
		ProjectDao projectDao = tx.projectDao();
		return projectDao.getETag(project, ac);
	}

	@Override
	public Page<? extends Project> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return ctx.tx().projectDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}
}
