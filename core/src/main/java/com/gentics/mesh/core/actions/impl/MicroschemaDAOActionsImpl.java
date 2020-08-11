package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class MicroschemaDAOActionsImpl implements MicroschemaDAOActions {

	@Inject
	public MicroschemaDAOActionsImpl() {
	}

	@Override
	public Microschema loadByUuid(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		MicroschemaDaoWrapper microschemaDao = tx.data().microschemaDao();
		if (perm == null) {
			return microschemaDao.findByUuid(uuid);
		} else {
			return microschemaDao.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
		}
	}
	
	@Override
	public Microschema loadByName(Tx tx, InternalActionContext ac, String name, GraphPermission perm, boolean errorIfNotFound) {
		MicroschemaDaoWrapper microschemaDao = tx.data().microschemaDao();
		if (perm == null) {
			return microschemaDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public TransformablePage<? extends Microschema> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return tx.data().microschemaDao().findAll(ac, pagingInfo);
		// return ac.getProject().getMicroschemaContainerRoot().findAll(ac2, pagingInfo);
	}

	@Override
	public TransformablePage<? extends Microschema> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<Microschema> extraFilter) {
		return tx.data().microschemaDao().findAll(ac, pagingInfo, extraFilter);
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

	public void delete(Tx tx, Microschema element, BulkActionContext bac) {
		// tx.data().microschemaDao()
		throw new RuntimeException("Delete not implemented");
	}

	@Override
	public MicroschemaResponse transformToRestSync(Tx tx, Microschema element, InternalActionContext ac, int level, String... languageTags) {
		return element.transformToRestSync(ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, Microschema element) {
		return element.getAPIPath(ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, Microschema element) {
		return element.getETag(ac);
	}

}
