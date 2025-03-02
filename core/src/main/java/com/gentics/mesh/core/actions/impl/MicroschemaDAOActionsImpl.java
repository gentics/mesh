package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * @see MicroschemaDAOActions
 */
@Singleton
public class MicroschemaDAOActionsImpl implements MicroschemaDAOActions {

	@Inject
	public MicroschemaDAOActionsImpl() {
	}

	@Override
	public HibMicroschema loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		MicroschemaDao microschemaDao = ctx.tx().microschemaDao();
		if (perm == null) {
			return microschemaDao.findByUuid(uuid);
		} else {
			return microschemaDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibMicroschema loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		MicroschemaDao microschemaDao = ctx.tx().microschemaDao();
		if (perm == null) {
			return microschemaDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public Page<? extends HibMicroschema> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.tx().microschemaDao().findAll(ctx.ac(), pagingInfo);
		// return ac.getProject().getMicroschemaContainerRoot().findAll(ac2, pagingInfo);
	}

	@Override
	public Page<? extends HibMicroschema> loadAll(DAOActionContext ctx, PagingParameters pagingInfo,
		Predicate<HibMicroschema> extraFilter) {
		return ctx.tx().microschemaDao().findAll(ctx.ac(), pagingInfo, schema -> {
			return extraFilter.test(schema);
		});
	}

	@Override
	public HibMicroschema create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.microschemaDao().create(ac, batch, uuid);
	}

	@Override
	public boolean update(Tx tx, HibMicroschema element, InternalActionContext ac, EventQueueBatch batch) {
		// Microschemas are updated via migrations
		return false;
	}

	@Override
	public void delete(Tx tx, HibMicroschema element) {
		tx.microschemaDao().delete(element);
	}

	@Override
	public MicroschemaResponse transformToRestSync(Tx tx, HibMicroschema element, InternalActionContext ac, int level, String... languageTags) {
		return tx.microschemaDao().transformToRestSync(element, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibMicroschema element) {
		return element.getAPIPath(ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibMicroschema element) {
		return element.getETag(ac);
	}

	@Override
	public Page<? extends HibMicroschema> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		return ctx.tx().microschemaDao().findAll(ctx.ac(), pagingInfo, extraFilter);
	}
}
