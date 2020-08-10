package com.gentics.mesh.core.endpoint.tagfamily;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.verticle.handler.CRUDActions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public class TagFamilyCrudActions implements CRUDActions<TagFamily, TagFamilyResponse> {

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
		//TagFamilyDaoWrapper tagFamilyDao = tx.data().tagFamilyDao();
		tagFamily.delete(bac);
	}

	@Override
	public TagFamilyResponse transformToRestSync(Tx tx, TagFamily element, InternalActionContext ac) {
		TagFamilyDaoWrapper tagFamilyDao = tx.data().tagFamilyDao();
		return tagFamilyDao.transformToRestSync(element, ac, 0);
	}

}
