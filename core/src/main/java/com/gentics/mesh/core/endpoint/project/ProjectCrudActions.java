package com.gentics.mesh.core.endpoint.project;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.verticle.handler.CRUDActions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class ProjectCrudActions implements CRUDActions<Project, ProjectResponse> {

	@Inject
	public ProjectCrudActions() {
	}

	@Override
	public Project load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		if (perm == null) {
			return tx.data().projectDao().findByUuid(uuid);
		} else {
			return tx.data().projectDao().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public TransformablePage<? extends Project> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return tx.data().projectDao().findAll(ac, pagingInfo);
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
	public ProjectResponse transformToRestSync(Tx tx, Project project, InternalActionContext ac) {
		return tx.data().projectDao().transformToRestSync(project, ac, 0);
	}
}
