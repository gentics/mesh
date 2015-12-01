package com.gentics.mesh.core.data.impl;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class MicroschemaContainerImpl extends AbstractGenericVertex<MicroschemaResponse> implements MicroschemaContainer {

	@Override
	public String getType() {
		return MicroschemaContainer.TYPE;
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public GenericVertex<MicroschemaResponse> transformToRest(InternalActionContext ac, Handler<AsyncResult<MicroschemaResponse>> handler) {
		MicroschemaResponse response = new MicroschemaResponse();
		// fillRest(response, rc);
		handler.handle(Future.succeededFuture(response));
		return this;
	}

	@Override
	public void delete() {
		throw new NotImplementedException();
	}

	@Override
	public void update(InternalActionContext rc, Handler<AsyncResult<Void>> handler) {
		throw new NotImplementedException();
	}

}
