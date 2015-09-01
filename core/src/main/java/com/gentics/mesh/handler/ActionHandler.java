package com.gentics.mesh.handler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public interface ActionHandler<T> extends Handler<Future<T>> {

	public static void fail(ActionContext ac, HttpResponseStatus status, String msg, String... parameters) {

	}

}
