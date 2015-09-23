package com.gentics.mesh.handler;

import com.gentics.mesh.handler.impl.InternalHttpActionContextImpl;

import io.vertx.ext.web.RoutingContext;

public interface InternalHttpActionContext extends HttpActionContext, InternalActionContext {

	public static InternalHttpActionContext create(RoutingContext rc) {
		return new InternalHttpActionContextImpl(rc);
	}

}
