package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class GroupDAOActionsImpl implements GroupDAOActions {

	@Inject
	public GroupDAOActionsImpl() {
	}

	@Override
	public HibGroup loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		GroupDaoWrapper groupDao = ctx.tx().data().groupDao();
		if (perm == null) {
			return groupDao.findByUuid(uuid);
		} else {
			return groupDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibGroup loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		GroupDaoWrapper groupDao = ctx.tx().data().groupDao();
		if (perm == null) {
			return groupDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public TransformablePage<? extends HibGroup> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.tx().data().groupDao().findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibGroup> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<HibGroup> extraFilter) {
		return ctx.tx().data().groupDao().findAll(ctx.ac(), pagingInfo, group -> {
			return extraFilter.test(group);
		});
	}

	@Override
	public HibGroup create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.data().groupDao().create(ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, HibGroup group, BulkActionContext bac) {
		tx.data().groupDao().delete(group, bac);
	}

	@Override
	public boolean update(Tx tx, HibGroup group, InternalActionContext ac, EventQueueBatch batch) {
		return tx.data().groupDao().update(group, ac, batch);
	}

	@Override
	public GroupResponse transformToRestSync(Tx tx, HibGroup group, InternalActionContext ac, int level, String... languageTags) {
		return tx.data().groupDao().transformToRestSync(group, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibGroup group) {
		return tx.data().groupDao().getAPIPath(group, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibGroup group) {
		return tx.data().groupDao().getETag(group, ac);
	}
}
