package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.PathParameters;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

@Singleton
public class TagDAOActionsImpl implements TagDAOActions {

	private Lazy<BootstrapInitializer> boot;

	@Inject
	public TagDAOActionsImpl(Lazy<BootstrapInitializer> boot) {
		this.boot  = boot;
	}

	@Override
	public Tag loadByUuid(DAOActionContext ctx, String tagUuid, GraphPermission perm, boolean errorIfNotFound) {
		TagFamily tagFamily = ctx.parent();
		if (perm == null) {
			return tagFamily.findByUuid(tagUuid);
			// TagDaoWrapper tagDao = tx.data().tagDao();
			// return tagDao.findByUuid(tagFamily, tagUuid);
		} else {
			return tagFamily.loadObjectByUuid(ctx.ac(), tagUuid, perm, errorIfNotFound);
		}
	}

	@Override
	public Tag loadByName(DAOActionContext ctx, String name, GraphPermission perm, boolean errorIfNotFound) {
		TagDaoWrapper tagDao = ctx.tx().data().tagDao();
		TagFamily tagFamily = ctx.parent();
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
	public TransformablePage<? extends Tag> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		// TagDaoWrapper tagDao = tx.data().tagDao();
		TagFamily tagFamily = ctx.parent();
		if(tagFamily != null) {
			return tagFamily.findAll(ctx.ac(), pagingInfo);
		} else {
			return boot.get().tagRoot().findAll(ctx.ac(), pagingInfo);
		}
	}

	@Override
	public Page<? extends Tag> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<Tag> extraFilter) {
		String tagFamilyUuid = PathParameters.getTagFamilyUuid(ctx.ac());
		return getTagFamily(ctx.tx(), ctx.ac(), tagFamilyUuid).findAll(ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public boolean update(Tx tx, Tag element, InternalActionContext ac, EventQueueBatch batch) {
		TagDaoWrapper tagDao = tx.data().tagDao();
		return tagDao.update(element, ac, batch);
	}

	@Override
	public Tag create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String tagUuid) {
		// TagDaoWrapper tagDao = tx.data().tagDao();
		String tagFamilyUuid = PathParameters.getTagFamilyUuid(ac);
		if (tagUuid == null) {
			return getTagFamily(tx, ac, tagFamilyUuid).create(ac, batch);
		} else {
			return getTagFamily(tx, ac, tagFamilyUuid).create(ac, batch, tagUuid);
		}
	}

	@Override
	public void delete(Tx tx, Tag tag, BulkActionContext bac) {
		// TagDaoWrapper tagDao = tx.data().tagDao();
		// TODO use dao
		tag.delete(bac);
	}

	@Override
	public TagResponse transformToRestSync(Tx tx, Tag tag, InternalActionContext ac, int level, String... languageTags) {
		// TagDaoWrapper tagDao = tx.data().tagDao();
		return tag.transformToRestSync(ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, Tag tag) {
		return tag.getAPIPath(ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, Tag tag) {
		return tag.getETag(ac);
	}

	private TagFamily getTagFamily(Tx tx, InternalActionContext ac, String tagFamilyUuid) {
		return tx.data().tagFamilyDao().findByUuid(ac.getProject(), tagFamilyUuid);
	}

}
