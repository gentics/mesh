package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * @see TagFamilyDAOActions
 */
@Singleton
public class TagFamilyDAOActionsImpl implements TagFamilyDAOActions {

	@Inject
	public TagFamilyDAOActionsImpl() {
	}

	@Override
	public HibTagFamily loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm,
			boolean errorIfNotFound) {
		TagFamilyDao tagFamilyDao = ctx.tx().tagFamilyDao();
		HibProject project = ctx.project();
		if (perm == null) {
			return tagFamilyDao.findByUuid(project, uuid);
		} else {
			return tagFamilyDao.loadObjectByUuid(project, ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibTagFamily loadByName(DAOActionContext ctx, String name, InternalPermission perm,
			boolean errorIfNotFound) {
		TagFamilyDao tagFamilyDao = ctx.tx().tagFamilyDao();
		if (perm == null) {
			return tagFamilyDao.findByName(ctx.project(), name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public Page<? extends HibTagFamily> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		HibProject project = ctx.project();
		TagFamilyDao tagFamilyDao = ctx.tx().tagFamilyDao();
		return tagFamilyDao.findAll(project, ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibTagFamily> loadAll(DAOActionContext ctx, PagingParameters pagingInfo,
			Predicate<HibTagFamily> extraFilter) {
		HibProject project = ctx.project();
		TagFamilyDao tagFamilyDao = ctx.tx().tagFamilyDao();
		return tagFamilyDao.findAll(project, ctx.ac(), pagingInfo, tagFamily -> {
			return extraFilter.test(tagFamily);
		});
	}

	@Override
	public boolean update(Tx tx, HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch) {
		TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
		return tagFamilyDao.update(tagFamily, ac, batch);
	}

	@Override
	public HibTagFamily create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
		return tagFamilyDao.create(tx.getProject(ac), ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, HibTagFamily tagFamily) {
		TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
		tagFamilyDao.delete(tagFamily);
	}

	@Override
	public TagFamilyResponse transformToRestSync(Tx tx, HibTagFamily element, InternalActionContext ac, int level,
			String... languageTags) {
		TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
		return tagFamilyDao.transformToRestSync(element, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibTagFamily tagFamily) {
		TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
		return tagFamilyDao.getAPIPath(tagFamily, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibTagFamily tagFamily) {
		TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
		return tagFamilyDao.getETag(tagFamily, ac);
	}

	@Override
	public Page<? extends HibTagFamily> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return ctx.tx().tagFamilyDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}
}
