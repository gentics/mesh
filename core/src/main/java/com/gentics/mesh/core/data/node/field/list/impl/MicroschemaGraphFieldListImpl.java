package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.impl.nesting.MicroschemaGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicroschemaGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.MicroschemaFieldListImpl;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class MicroschemaGraphFieldListImpl extends AbstractReferencingGraphFieldList<MicroschemaGraphField, MicroschemaFieldListImpl>
		implements MicroschemaGraphFieldList {

	@Override
	public Class<? extends MicroschemaGraphField> getListType() {
		return MicroschemaGraphFieldImpl.class;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

	@Override
	public void transformToRest(InternalActionContext ac, String fieldKey, Handler<AsyncResult<MicroschemaFieldListImpl>> handler) {
		handler.handle(Future.succeededFuture(null));
	}

}
