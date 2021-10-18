package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * @see SchemaDAOActions
 */
@Singleton
public class SchemaDAOActionsImpl implements SchemaDAOActions {

	@Inject
	public SchemaDAOActionsImpl() {
	}

	@Override
	public HibSchema loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		SchemaDao schemaDao = ctx.tx().schemaDao();
		if (perm == null) {
			return schemaDao.findByUuid(uuid);
		} else {
			return schemaDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibSchema loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		SchemaDao schemaDao = ctx.tx().schemaDao();
		if (perm == null) {
			return schemaDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	/**
	 * Load all schemas that are linked to the project.
	 * 
	 * @param tx
	 * @param project
	 * @param ac
	 * @param pagingInfo
	 * @return
	 */
	public Page<? extends HibSchema> loadAll(Tx tx, HibProject project, InternalActionContext ac, PagingParameters pagingInfo) {
		SchemaDao schemaDao = tx.schemaDao();
		return schemaDao.findAll(project, ac, pagingInfo);
	}

	@Override
	public Page<? extends HibSchema> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		SchemaDao schemaDao = ctx.tx().schemaDao();
		return schemaDao.findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibSchema> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<HibSchema> extraFilter) {
		SchemaDao schemaDao = ctx.tx().schemaDao();
		return schemaDao.findAll(ctx.project(), ctx.ac(), pagingInfo, schema -> {
			return extraFilter.test(schema);
		});
		// TODO scope to project
		// return tx.schemaDao().findAll(ac, pagingInfo, extraFilter);
	}

	@Override
	public boolean update(Tx tx, HibSchema element, InternalActionContext ac, EventQueueBatch batch) {
		// Updates are handled by dedicated migration code
		return false;
	}

	@Override
	public HibSchema create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.schemaDao().create(ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, HibSchema schema, BulkActionContext bac) {
		tx.schemaDao().delete(schema, bac);
	}

	@Override
	public SchemaResponse transformToRestSync(Tx tx, HibSchema schema, InternalActionContext ac, int level, String... languageTags) {
		return tx.schemaDao().transformToRestSync(schema, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibSchema schema) {
		return schema.getAPIPath(ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibSchema schema) {
		return schema.getETag(ac);
	}

}
