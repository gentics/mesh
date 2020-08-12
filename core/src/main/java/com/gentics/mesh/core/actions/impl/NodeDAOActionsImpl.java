package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.NodeDAOActions;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
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
	public Node loadByUuid(DAOActionContext ctx, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		if (perm == null) {
			return ctx.project().getNodeRoot().findByUuid(uuid);
		} else {
			return ctx.project().getNodeRoot().loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}
	
	@Override
	public Node loadByName(DAOActionContext ctx, String name, GraphPermission perm, boolean errorIfNotFound) {
		//NodeDaoWrapper nodeDao = tx.data().nodeDao();
		if (perm == null) {
			return ctx.project().getNodeRoot().findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public TransformablePage<? extends Node> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return ac.getProject().getNodeRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends Node> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<Node> extraFilter) {
		return ctx.project().getNodeRoot().findAll(ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public boolean update(Tx tx, Node element, InternalActionContext ac, EventQueueBatch batch) {
		return element.update(ac, batch);
		// return ac.getProject().getNodeRoot().update(element, ac, batch);
	}

	@Override
	public Node create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return ac.getProject().getNodeRoot().create(ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, Node node, BulkActionContext bac) {
		// tx.data().nodeDao();
		node.delete(bac);
	}

	@Override
	public NodeResponse transformToRestSync(Tx tx, Node node, InternalActionContext ac, int level, String... languageTags) {
		// tx.data().nodeDao()
		return node.transformToRestSync(ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, Node node) {
		return node.getAPIPath(ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, Node node) {
		return node.getETag(ac);
	}

}
