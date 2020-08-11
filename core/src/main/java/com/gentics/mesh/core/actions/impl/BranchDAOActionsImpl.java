package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.actions.BranchDAOActions;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
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
	public Branch load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		// BranchDaoWrapper branchDao = tx.data().branchDao();
		// TODO use DAO
		return ac.getProject().getBranchRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public TransformablePage<? extends Branch> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		// BranchDaoWrapper branchDao = tx.data().branchDao();
		// TODO use DAO
		return ac.getProject().getBranchRoot().findAll(ac, pagingInfo);
	}

	@Override
	public TransformablePage<? extends Branch> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo, Predicate<Branch> extraFilter) {
		// BranchDaoWrapper branchDao = tx.data().branchDao();
		// TODO use DAO
		return ac.getProject().getBranchRoot().findAll(ac, pagingInfo, extraFilter);
	}

	@Override
	public Branch create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		// BranchDaoWrapper branchDao = tx.data().branchDao();
		// TODO use DAO
		return ac.getProject().getBranchRoot().create(ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, Branch branch, InternalActionContext ac, EventQueueBatch batch) {
		// BranchDaoWrapper branchDao = tx.data().branchDao();
		// TODO use DAO
		return branch.update(ac, batch);
	}

	@Override
	public void delete(Tx tx, Branch element, BulkActionContext bac) {
		// Branches are not deletable
	}

	@Override
	public BranchResponse transformToRestSync(Tx tx, Branch branch, InternalActionContext ac, int level, String... languageTags) {
		BranchDaoWrapper branchDao = tx.data().branchDao();
		return branchDao.transformToRestSync(branch, ac, level, languageTags);
	}

}
