package com.gentics.mesh.core.actions.impl;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.actions.SchemaDAOActions;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
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
	public Schema load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return tx.data().schemaDao().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public TransformablePage<? extends Schema> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return ac.getProject().getSchemaContainerRoot().findAll(ac, pagingInfo);
		// TODO scope to project
		// return tx.data().schemaDao().findAll(ac, pagingInfo);
	}

	@Override
	public TransformablePage<? extends Schema> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo, Predicate<Schema> extraFilter) {
		return ac.getProject().getSchemaContainerRoot().findAll(ac, pagingInfo, extraFilter);
		// TODO scope to project
		// return tx.data().schemaDao().findAll(ac, pagingInfo, extraFilter);
	}

	@Override
	public boolean update(Tx tx, Schema element, InternalActionContext ac, EventQueueBatch batch) {
		// Updates are handled by dedicated migration code
		return false;
	}

	@Override
	public Schema create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return tx.data().schemaDao().create(ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, Schema schema, BulkActionContext bac) {
		tx.data().schemaDao().delete(schema, bac);
	}

	@Override
	public SchemaResponse transformToRestSync(Tx tx, Schema schema, InternalActionContext ac, int level, String... languageTags) {
		// return tx.data().schemaDao().
		return schema.transformToRestSync(ac, level, languageTags);
	}

}
