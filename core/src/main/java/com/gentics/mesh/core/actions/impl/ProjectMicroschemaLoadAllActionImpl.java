package com.gentics.mesh.core.actions.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.LoadAllAction;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.parameter.PagingParameters;

@Singleton
public class ProjectMicroschemaLoadAllActionImpl implements LoadAllAction<Microschema> {
	@Inject
	public ProjectMicroschemaLoadAllActionImpl() {
	}

	@Override
	public TransformablePage<? extends Microschema> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		return ctx.project().getMicroschemaContainerRoot().findAll(ctx.ac(), pagingInfo);
	}
}
