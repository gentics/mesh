package com.gentics.mesh.core.actions.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toSchema;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class SchemaDAOActionsImpl implements SchemaDAOActions {

	@Inject
	public SchemaDAOActionsImpl() {
	}

	@Override
	public HibSchema loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		SchemaDaoWrapper schemaDao = ctx.tx().data().schemaDao();
		if (perm == null) {
			return schemaDao.findByUuid(uuid);
		} else {
			return schemaDao.loadObjectByUuid(ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibSchema loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		SchemaDaoWrapper schemaDao = ctx.tx().data().schemaDao();
		if (perm == null) {
			return schemaDao.findByName(name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	public TransformablePage<? extends HibSchema> loadAll(Tx tx, Project project, InternalActionContext ac, PagingParameters pagingInfo) {
		SchemaDaoWrapper schemaDao = tx.data().schemaDao();
		return schemaDao.findAll(ac, project, pagingInfo);
	}

	@Override
	public TransformablePage<? extends HibSchema> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		SchemaDaoWrapper schemaDao = ctx.tx().data().schemaDao();
		return schemaDao.findAll(ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibSchema> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<HibSchema> extraFilter) {
		return ctx.project().getSchemaContainerRoot().findAll(ctx.ac(), pagingInfo, schema -> {
			return extraFilter.test(schema);
		});
		// TODO scope to project
		// return tx.data().schemaDao().findAll(ac, pagingInfo, extraFilter);
	}

	@Override
	public boolean update(Tx tx, HibSchema element, InternalActionContext ac, EventQueueBatch batch) {
		// Updates are handled by dedicated migration code
		return false;
	}

	@Override
	public HibSchema create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.data().schemaDao().create(ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, HibSchema schema, BulkActionContext bac) {
		tx.data().schemaDao().delete(schema, bac);
	}

	@Override
	public SchemaResponse transformToRestSync(Tx tx, HibSchema schema, InternalActionContext ac, int level, String... languageTags) {
		Schema graphSchema = toSchema(schema);
		return graphSchema.transformToRestSync(ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibSchema schema) {
		Schema graphSchema = toSchema(schema);
		return graphSchema.getAPIPath(ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibSchema schema) {
		Schema graphSchema = toSchema(schema);
		return graphSchema.getETag(ac);
	}

}
