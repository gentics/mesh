package com.gentics.mesh.rest.method;

import io.vertx.core.Handler;
import io.vertx.core.http.WebSocket;

public interface EventbusClientMethods {

	/**
	 * Connect to the mesh eventbus bridge via a websocket.
	 * 
	 * @param wsConnect
	 */
	void eventbus(Handler<WebSocket> wsConnect);
}
