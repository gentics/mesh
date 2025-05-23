package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.PathParameters;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * @see TagDAOActions
 */
@Singleton
public class TagDAOActionsImpl implements TagDAOActions {

	@Inject
	public TagDAOActionsImpl() {
	}

	@Override
	public HibTag loadByUuid(DAOActionContext ctx, String tagUuid, InternalPermission perm, boolean errorIfNotFound) {
		TagDao tagDao = ctx.tx().tagDao();
		HibTagFamily tagFamily = ctx.parent();
		if (perm == null) {
			return tagDao.findByUuid(tagFamily, tagUuid);
		} else {
			return tagDao.loadObjectByUuid(tagFamily, ctx.ac(), tagUuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibTag loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		TagDao tagDao = ctx.tx().tagDao();
		HibTagFamily tagFamily = ctx.parent();
		if (perm == null) {
			if (tagFamily == null) {
				return tagDao.findByName(name);
			} else {
				return tagDao.findByName(tagFamily, name);
			}
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public Page<? extends HibTag> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		HibTagFamily hibTagFamily = ctx.parent();
		TagDao tagDao = Tx.get().tagDao();
		if (hibTagFamily != null) {
			return tagDao.findAll(hibTagFamily, ctx.ac(), pagingInfo);
		} else {
			return tagDao.findAll(ctx.ac(), pagingInfo);
		}
	}

	@Override
	public Page<? extends HibTag> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<HibTag> extraFilter) {
		HibTagFamily hibTagFamily = ctx.parent();
		TagDao tagDao = Tx.get().tagDao();
		if (hibTagFamily != null) {
			return ctx.tx().tagDao().findAll(hibTagFamily, ctx.ac(), pagingInfo, extraFilter);
		} else {
			return tagDao.findAll(ctx.ac(), pagingInfo);
		}
	}

	@Override
	public boolean update(Tx tx, HibTag element, InternalActionContext ac, EventQueueBatch batch) {
		TagDao tagDao = tx.tagDao();
		return tagDao.update(element, ac, batch);
	}

	@Override
	public HibTag create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String tagUuid) {
		// TODO add parent uuid parameter and utilize it instead of extracting it from ac
		TagDao tagDao = tx.tagDao();
		String tagFamilyUuid = PathParameters.getTagFamilyUuid(ac);
		HibTagFamily tagFamily = getTagFamily(tx, ac, tagFamilyUuid);
		if (tagUuid == null) {
			return tagDao.create(tagFamily, ac, batch);
		} else {
			return tagDao.create(tagFamily, ac, batch, tagUuid);
		}
	}

	@Override
	public void delete(Tx tx, HibTag tag) {
		TagDao tagDao = tx.tagDao();
		tagDao.delete(tag);
	}

	@Override
	public TagResponse transformToRestSync(Tx tx, HibTag tag, InternalActionContext ac, int level, String... languageTags) {
		TagDao tagDao = tx.tagDao();
		return tagDao.transformToRestSync(tag, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibTag tag) {
		TagDao tagDao = tx.tagDao();
		return tagDao.getAPIPath(tag, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibTag tag) {
		TagDao tagDao = tx.tagDao();
		return tagDao.getETag(tag, ac);
	}

	private HibTagFamily getTagFamily(Tx tx, InternalActionContext ac, String tagFamilyUuid) {
		return tx.tagFamilyDao().findByUuid(tx.getProject(ac), tagFamilyUuid);
	}

	@Override
	public Page<? extends HibTag> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return ctx.tx().tagDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}
}
