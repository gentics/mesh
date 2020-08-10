package com.gentics.mesh.core.endpoint.user;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.handler.CRUDActions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public class UserCrudActions implements CRUDActions<HibUser, UserResponse> {

	@Override
	public HibUser load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return tx.data().userDao().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public TransformablePage<? extends HibUser> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return tx.data().userDao().findAll(ac, pagingInfo);
	}

	@Override
	public HibUser create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.data().userDao().create(ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, HibUser user, BulkActionContext bac) {
		tx.data().userDao().delete(user, bac);
	}

	@Override
	public boolean update(Tx tx, HibUser user, InternalActionContext ac, EventQueueBatch batch) {
		return tx.data().userDao().update(user, ac, batch);
	}

	@Override
	public UserResponse transformToRestSync(Tx tx, HibUser user, InternalActionContext ac) {
		return tx.data().userDao().transformToRestSync(user, ac, 0);
	}
}
