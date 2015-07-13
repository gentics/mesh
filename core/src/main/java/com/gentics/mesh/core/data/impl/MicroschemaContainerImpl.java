package com.gentics.mesh.core.data.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.data.service.transformation.TransformationParameters;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;

public class MicroschemaContainerImpl extends AbstractGenericNode<MicroschemaResponse> implements MicroschemaContainer {

	@Override
	public GenericNode<MicroschemaResponse> transformToRest(MeshAuthUser requestUser, Handler<AsyncResult<MicroschemaResponse>> handler, TransformationParameters... parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
		
	}

}
