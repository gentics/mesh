package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.BranchDAOActions;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class BranchDAOActionsImpl implements BranchDAOActions {

	@Inject
	public BranchDAOActionsImpl() {
	}

	@Override
	public HibBranch loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		BranchDaoWrapper branchDao = ctx.tx().branchDao();
		if (perm == null) {
			return branchDao.findByUuid(ctx.project(), uuid);
		} else {
			return branchDao.loadObjectByUuid(ctx.project(), ctx.ac(), uuid, perm, errorIfNotFound);

		}
	}

	@Override
	public HibBranch loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		if (perm == null) {
			BranchDaoWrapper branchDao = ctx.tx().branchDao();
			return branchDao.findByName(ctx.project(), name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public TransformablePage<? extends HibBranch> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		BranchDaoWrapper branchDao = ctx.tx().branchDao();
		return branchDao.findAll(ctx.project(), ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibBranch> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<HibBranch> extraFilter) {
		BranchDaoWrapper branchDao = ctx.tx().branchDao();
		return branchDao.findAll(ctx.project(), ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public HibBranch create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		BranchDaoWrapper branchDao = tx.branchDao();
		return branchDao.create(tx.getProject(ac), ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, HibBranch branch, InternalActionContext ac, EventQueueBatch batch) {
		BranchDaoWrapper branchDao = tx.branchDao();
		return branchDao.update(branch, ac, batch);
	}

	@Override
	public void delete(Tx tx, HibBranch element, BulkActionContext bac) {
		throw new RuntimeException("Branches are currently not deletable");
	}

	@Override
	public BranchResponse transformToRestSync(Tx tx, HibBranch branch, InternalActionContext ac, int level, String... languageTags) {
		BranchDaoWrapper branchDao = tx.branchDao();
		return branchDao.transformToRestSync(branch, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibBranch branch) {
		BranchDaoWrapper branchDao = tx.branchDao();
		return branchDao.getAPIPath(branch, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibBranch branch) {
		BranchDaoWrapper branchDao = tx.branchDao();
		return branchDao.getETag(branch, ac);
	}

}
