package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * @see RoleDAOActions
 */
@Singleton
public class RoleDAOActionsImpl implements RoleDAOActions {

	@Inject
	public RoleDAOActionsImpl() {
	}

	@Override
	public Role loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		RoleDao roleDao = ctx.tx().roleDao();
		if (perm == null) {
			return roleDao.findByUuid(uuid);
		} else {
			return roleDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public Role loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		RoleDao roleDao = ctx.tx().roleDao();
		if (perm == null) {
			return roleDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public Page<? extends Role> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.tx().roleDao().findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends Role> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<Role> extraFilter) {
		return ctx.tx().roleDao().findAll(ctx.ac(), pagingInfo, role -> {
			return extraFilter.test(role);
		});
	}

	@Override
	public Role create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.roleDao().create(ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, Role role, InternalActionContext ac, EventQueueBatch batch) {
		return tx.roleDao().update(role, ac, batch);
	}

	@Override
	public void delete(Tx tx, Role role, BulkActionContext bac) {
		tx.roleDao().delete(role, bac);
	}

	@Override
	public RoleResponse transformToRestSync(Tx tx, Role role, InternalActionContext ac, int level, String... languageTags) {
		return tx.roleDao().transformToRestSync(role, ac, 0);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, Role role) {
		return role.getAPIPath(ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, Role role) {
		return role.getETag(ac);
	}

	@Override
	public Page<? extends Role> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return ctx.tx().roleDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}
}
