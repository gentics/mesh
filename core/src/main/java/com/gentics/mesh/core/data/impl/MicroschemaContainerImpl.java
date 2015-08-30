package com.gentics.mesh.core.data.impl;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class MicroschemaContainerImpl extends AbstractGenericVertex<MicroschemaResponse>implements MicroschemaContainer {

	@Override
	public String getType() {
		return MicroschemaContainer.TYPE;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public GenericVertex<MicroschemaResponse> transformToRest(RoutingContext rc, Handler<AsyncResult<MicroschemaResponse>> handler) {
		MicroschemaResponse response = new MicroschemaResponse();
		//fillRest(response, rc);
		handler.handle(Future.succeededFuture(response));
		return this;
	}

	@Override
	public void delete() {
		throw new NotImplementedException();
	}

	@Override
	public SearchQueueBatch update(RoutingContext rc) {
		throw new NotImplementedException();
	}

}
