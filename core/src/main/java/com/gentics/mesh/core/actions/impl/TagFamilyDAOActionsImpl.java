package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.actions.TagFamilyDAOActions;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class TagFamilyDAOActionsImpl implements TagFamilyDAOActions {

	@Inject
	public TagFamilyDAOActionsImpl() {
	}

	@Override
	public TagFamily load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		if (perm == null) {
			return ac.getProject().getTagFamilyRoot().findByUuid(uuid);
		} else {
			return ac.getProject().getTagFamilyRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public TransformablePage<? extends TagFamily> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return ac.getProject().getTagFamilyRoot().findAll(ac, pagingInfo);
	}

	@Override
	public TransformablePage<? extends TagFamily> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<TagFamily> extraFilter) {
		return ac.getProject().getTagFamilyRoot().findAll(ac, pagingInfo, extraFilter);
	}

	@Override
	public boolean update(Tx tx, TagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch) {
		TagFamilyDaoWrapper tagFamilyDao = tx.data().tagFamilyDao();
		return tagFamilyDao.update(tagFamily, ac, batch);
	}

	@Override
	public TagFamily create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		TagFamilyDaoWrapper tagFamilyDao = tx.data().tagFamilyDao();
		return tagFamilyDao.create(ac.getProject(), ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, TagFamily tagFamily, BulkActionContext bac) {
		// TagFamilyDaoWrapper tagFamilyDao = tx.data().tagFamilyDao();
		tagFamily.delete(bac);
	}

	@Override
	public TagFamilyResponse transformToRestSync(Tx tx, TagFamily element, InternalActionContext ac, int level, String... languageTags) {
		TagFamilyDaoWrapper tagFamilyDao = tx.data().tagFamilyDao();
		return tagFamilyDao.transformToRestSync(element, ac, level, languageTags);
	}

}
