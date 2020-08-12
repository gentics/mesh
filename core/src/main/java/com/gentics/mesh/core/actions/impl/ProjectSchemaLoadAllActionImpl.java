package com.gentics.mesh.core.actions.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.LoadAllAction;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class ProjectSchemaLoadAllActionImpl implements LoadAllAction<Schema> {
	@Inject
	public ProjectSchemaLoadAllActionImpl() {
	}

	@Override
	public TransformablePage<? extends Schema> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo) {
		return tx.data().schemaDao().findAll(ac, ac.getProject(), pagingInfo);
	}
}
