package com.gentics.mesh.core.endpoint.group;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.verticle.handler.CRUDActions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public class GroupCrudActions implements CRUDActions<Group, GroupResponse> {

	@Override
	public Group load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return tx.data().groupDao().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public TransformablePage<? extends Group> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return tx.data().groupDao().findAll(ac, pagingInfo);
	}

	@Override
	public Group create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.data().groupDao().create(ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, Group group, BulkActionContext bac) {
		tx.data().groupDao().delete(group, bac);
	}

	@Override
	public boolean update(Tx tx, Group group, InternalActionContext ac, EventQueueBatch batch) {
		return tx.data().groupDao().update(group, ac, batch);
	}

	@Override
	public GroupResponse transformToRestSync(Tx tx, Group group, InternalActionContext ac) {
		return tx.data().groupDao().transformToRestSync(group, ac, 0);
	}

}
