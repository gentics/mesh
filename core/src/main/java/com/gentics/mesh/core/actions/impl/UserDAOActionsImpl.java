package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.action.DAOActionContext;
import com.gentics.mesh.core.data.action.UserDAOActions;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * @see UserDAOActions
 */
@Singleton
public class UserDAOActionsImpl implements UserDAOActions {

	@Inject
	public UserDAOActionsImpl() {
	}

	@Override
	public HibUser loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		UserDao userDao = ctx.tx().userDao();
		if (perm == null) {
			return userDao.findByUuid(uuid);
		} else {
			return userDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibUser loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		UserDao userDao = ctx.tx().userDao();
		if (perm == null) {
			return userDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public Page<? extends HibUser> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.tx().userDao().findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibUser> loadAll(DAOActionContext ctx, PagingParameters pagingInfo,
		Predicate<HibUser> extraFilter) {
		return ctx.tx().userDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public HibUser create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.userDao().create(ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, HibUser user, BulkActionContext bac) {
		tx.userDao().delete(user, bac);
	}

	@Override
	public boolean update(Tx tx, HibUser user, InternalActionContext ac, EventQueueBatch batch) {
		return tx.userDao().update(user, ac, batch);
	}

	@Override
	public UserResponse transformToRestSync(Tx tx, HibUser user, InternalActionContext ac, int level, String... languageTags) {
		return tx.userDao().transformToRestSync(user, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibUser user) {
		return tx.userDao().getAPIPath(user, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibUser user) {
		return tx.userDao().getETag(user, ac);
	}

}
