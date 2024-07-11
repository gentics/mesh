package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * @see GroupDAOActions
 */
@Singleton
public class GroupDAOActionsImpl implements GroupDAOActions {

	@Inject
	public GroupDAOActionsImpl() {
	}

	@Override
	public Group loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		GroupDao groupDao = ctx.tx().groupDao();
		if (perm == null) {
			return groupDao.findByUuid(uuid);
		} else {
			return groupDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public Group loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		GroupDao groupDao = ctx.tx().groupDao();
		if (perm == null) {
			return groupDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public Page<? extends Group> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.tx().groupDao().findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends Group> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<Group> extraFilter) {
		return ctx.tx().groupDao().findAll(ctx.ac(), pagingInfo, group -> {
			return extraFilter.test(group);
		});
	}

	@Override
	public Group create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.groupDao().create(ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, Group group, BulkActionContext bac) {
		tx.groupDao().delete(group, bac);
	}

	@Override
	public boolean update(Tx tx, Group group, InternalActionContext ac, EventQueueBatch batch) {
		return tx.groupDao().update(group, ac, batch);
	}

	@Override
	public GroupResponse transformToRestSync(Tx tx, Group group, InternalActionContext ac, int level, String... languageTags) {
		return tx.groupDao().transformToRestSync(group, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, Group group) {
		return tx.groupDao().getAPIPath(group, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, Group group) {
		return tx.groupDao().getETag(group, ac);
	}

	@Override
	public Page<? extends Group> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return ctx.tx().groupDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}
}
