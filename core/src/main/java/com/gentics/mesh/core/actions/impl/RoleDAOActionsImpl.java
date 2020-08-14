package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class RoleDAOActionsImpl implements RoleDAOActions {

	@Inject
	public RoleDAOActionsImpl() {
	}

	@Override
	public HibRole loadByUuid(DAOActionContext ctx, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		RoleDaoWrapper roleDao = ctx.tx().data().roleDao();
		if (perm == null) {
			return roleDao.findByUuid(uuid);
		} else {
			return roleDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibRole loadByName(DAOActionContext ctx, String name, GraphPermission perm, boolean errorIfNotFound) {
		RoleDaoWrapper roleDao = ctx.tx().data().roleDao();
		if (perm == null) {
			return roleDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public TransformablePage<? extends HibRole> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.tx().data().roleDao().findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibRole> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<HibRole> extraFilter) {
		return ctx.tx().data().roleDao().findAll(ctx.ac(), pagingInfo, role -> {
			return extraFilter.test(role);
		});
	}

	@Override
	public HibRole create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.data().roleDao().create(ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, HibRole role, InternalActionContext ac, EventQueueBatch batch) {
		return tx.data().roleDao().update(role, ac, batch);
	}

	@Override
	public void delete(Tx tx, HibRole role, BulkActionContext bac) {
		tx.data().roleDao().delete(role, bac);
	}

	@Override
	public RoleResponse transformToRestSync(Tx tx, HibRole role, InternalActionContext ac, int level, String... languageTags) {
		return tx.data().roleDao().transformToRestSync(role, ac, 0);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibRole role) {
		return tx.data().roleDao().getAPIPath(role, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibRole role) {
		return tx.data().roleDao().getETag(role, ac);
	}

}
