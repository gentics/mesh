package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class ProjectDAOActionsImpl implements ProjectDAOActions {

	@Inject
	public ProjectDAOActionsImpl() {
	}

	@Override
	public Project loadByUuid(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		ProjectDaoWrapper projectDao = tx.data().projectDao();
		if (perm == null) {
			return projectDao.findByUuid(uuid);
		} else {
			return projectDao.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public Project loadByName(Tx tx, InternalActionContext ac, String name, GraphPermission perm, boolean errorIfNotFound) {
		if (perm == null) {
			return tx.data().projectDao().findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public TransformablePage<? extends Project> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return tx.data().projectDao().findAll(ac, pagingInfo);
	}

	@Override
	public TransformablePage<? extends Project> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<Project> extraFilter) {
		return tx.data().projectDao().findAll(ac, pagingInfo, extraFilter);
	}

	@Override
	public Project create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.data().projectDao().create(ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, Project element, InternalActionContext ac, EventQueueBatch batch) {
		return tx.data().projectDao().update(element, ac, batch);
	}

	public void delete(Tx tx, Project project, BulkActionContext bac) {
		tx.data().projectDao().delete(project, bac);
	}

	@Override
	public ProjectResponse transformToRestSync(Tx tx, Project project, InternalActionContext ac, int level, String... languageTags) {
		return tx.data().projectDao().transformToRestSync(project, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, Project project) {
		return project.getAPIPath(ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, Project project) {
		return project.getETag(ac);
	}

}
