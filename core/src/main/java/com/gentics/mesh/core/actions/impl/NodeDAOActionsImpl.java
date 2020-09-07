package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.NodeDAOActions;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class NodeDAOActionsImpl implements NodeDAOActions {

	@Inject
	public NodeDAOActionsImpl() {
	}

	@Override
	public HibNode loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		if (perm == null) {
			return ctx.tx().data().nodeDao().findByUuid(ctx.project(), uuid);
		} else {
			return ctx.project().getNodeRoot().loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibNode loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		// NodeDaoWrapper nodeDao = tx.data().nodeDao();
		if (perm == null) {
			return ctx.project().getNodeRoot().findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public TransformablePage<? extends HibNode> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.project().getNodeRoot().findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibNode> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<HibNode> extraFilter) {
		NodeDaoWrapper nodeDao = ctx.tx().data().nodeDao();
		return nodeDao.findAll(ctx.project(), ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public boolean update(Tx tx, HibNode node, InternalActionContext ac, EventQueueBatch batch) {
		NodeDaoWrapper nodeDao = tx.data().nodeDao();
		return nodeDao.update(node, ac, batch);
	}

	@Override
	public HibNode create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		NodeDaoWrapper nodeDao = tx.data().nodeDao();
		HibProject project = tx.getProject(ac);
		return nodeDao.create(project, ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, HibNode node, BulkActionContext bac) {
		NodeDaoWrapper nodeDao = tx.data().nodeDao();
		nodeDao.delete(node, bac, false, true);
	}

	@Override
	public NodeResponse transformToRestSync(Tx tx, HibNode node, InternalActionContext ac, int level, String... languageTags) {
		NodeDaoWrapper nodeDao = tx.data().nodeDao();
		return nodeDao.transformToRestSync(node, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibNode node) {
		NodeDaoWrapper nodeDao = tx.data().nodeDao();
		return nodeDao.getAPIPath(node, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibNode node) {
		NodeDaoWrapper nodeDao = tx.data().nodeDao();
		return nodeDao.getETag(node, ac);
	}

}
