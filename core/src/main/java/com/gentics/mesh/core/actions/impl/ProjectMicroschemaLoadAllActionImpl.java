package com.gentics.mesh.core.actions.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.LoadAllAction;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class ProjectMicroschemaLoadAllActionImpl implements LoadAllAction<HibMicroschema> {
	@Inject
	public ProjectMicroschemaLoadAllActionImpl() {
	}

	@Override
	public Page<? extends HibMicroschema> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		MicroschemaDaoWrapper microschemaDao = ctx.tx().microschemaDao();
		return microschemaDao.findAll(ctx.project(), ctx.ac(), pagingInfo);
	}
}
