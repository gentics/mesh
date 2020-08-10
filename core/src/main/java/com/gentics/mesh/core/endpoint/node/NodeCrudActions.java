package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.handler.CRUDActions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public class NodeCrudActions implements CRUDActions<Node, NodeResponse> {

	@Override
	public Node load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		if (perm == null) {
			return ac.getProject().getNodeRoot().findByUuid(uuid);
		} else {
			return ac.getProject().getNodeRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public TransformablePage<? extends Node> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return ac.getProject().getNodeRoot().findAll(ac, pagingInfo);
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
	public NodeResponse transformToRestSync(Tx tx, Node node, InternalActionContext ac) {
		// tx.data().nodeDao()
		return node.transformToRestSync(ac, 0);
	}

}
