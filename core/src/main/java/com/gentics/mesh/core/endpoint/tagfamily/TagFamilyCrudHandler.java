package com.gentics.mesh.core.endpoint.tagfamily;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.verticle.handler.CreateAction;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.LoadAction;
import com.gentics.mesh.core.verticle.handler.LoadAllAction;
import com.gentics.mesh.core.verticle.handler.UpdateAction;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.graphdb.spi.Database;

@Singleton
public class TagFamilyCrudHandler extends AbstractCrudHandler<TagFamily, TagFamilyResponse> {

	@Inject
	public TagFamilyCrudHandler(Database db, HandlerUtilities utils, WriteLock writeLock) {
		super(db, utils, writeLock);
	}

	@Override
	public LoadAction<TagFamily> loadAction() {
		return (tx, ac, uuid, perm, errorIfNotFound) -> {
			if (perm == null) {
				return ac.getProject().getTagFamilyRoot().findByUuid(uuid);
			} else {
				return ac.getProject().getTagFamilyRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
			}
		};
	}

	@Override
	public LoadAllAction<TagFamily> loadAllAction() {
		return (tx, ac, pagingInfo) -> {
			return ac.getProject().getTagFamilyRoot().findAll(ac, pagingInfo);
		};
	}

	@Override
	public CreateAction<TagFamily> createAction() {
		return (tx, ac, batch, uuid) -> {
			TagFamilyDaoWrapper tagFamilyDao = tx.data().tagFamilyDao();
			return tagFamilyDao.create(ac.getProject(), ac, batch, uuid);
		};
	}

	@Override
	public UpdateAction<TagFamily> updateAction() {
		return (tx, tagFamily, ac, batch) -> {
			TagFamilyDaoWrapper tagFamilyDao = tx.data().tagFamilyDao();
			return tagFamilyDao.update(tagFamily, ac, batch);
		};
	}

}
