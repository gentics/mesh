package com.gentics.mesh.core.endpoint.branch;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.verticle.handler.CRUDActions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public class BranchCrudActions implements CRUDActions<Branch, BranchResponse> {

	@Override
	public Branch load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return ac.getProject().getBranchRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public TransformablePage<? extends Branch> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return ac.getProject().getBranchRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Branch create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return ac.getProject().getBranchRoot().create(ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, Branch branch, InternalActionContext ac, EventQueueBatch batch) {
		return branch.update(ac, batch);
	}

	@Override
	public void delete(Tx tx, Branch element, BulkActionContext bac) {
		// Branches are not deletable
	}
	
	@Override
	public BranchResponse transformToRestSync(Tx tx, Branch branch, InternalActionContext ac) {
		return tx.data().branchDao().transformToRestSync(branch, ac, 0);
	}

}
