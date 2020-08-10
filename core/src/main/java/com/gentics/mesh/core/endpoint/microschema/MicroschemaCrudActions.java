package com.gentics.mesh.core.endpoint.microschema;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.verticle.handler.CRUDActions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

public class MicroschemaCrudActions implements CRUDActions<Microschema, MicroschemaResponse> {

	@Override
	public Microschema load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return tx.data().microschemaDao().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public TransformablePage<? extends Microschema> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return tx.data().microschemaDao().findAll(ac, pagingInfo);
		//return ac.getProject().getMicroschemaContainerRoot().findAll(ac2, pagingInfo);
	}

	@Override
	public Microschema create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.data().microschemaDao().create(ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, Microschema element, InternalActionContext ac, EventQueueBatch batch) {
		// Microschemas are updated via migrations
		return false;
	}

	public void delete(Tx tx, Microschema element, com.gentics.mesh.context.BulkActionContext bac) {
	}
	
	@Override
	public MicroschemaResponse transformToRestSync(Tx tx, Microschema element, InternalActionContext ac) {
		// TODO Auto-generated method stub
		return null;
	}

}
