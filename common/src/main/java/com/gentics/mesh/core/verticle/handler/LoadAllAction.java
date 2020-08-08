package com.gentics.mesh.core.verticle.handler;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.parameter.PagingParameters;

@FunctionalInterface
public interface LoadAllAction<T> {

	TransformablePage<? extends T> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo);

}
