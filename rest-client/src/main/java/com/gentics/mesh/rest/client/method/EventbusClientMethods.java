package com.gentics.mesh.rest.client.method;

import io.vertx.core.Handler;
import io.vertx.core.http.WebSocket;

/**
 * Eventbus endpoint specific REST methods.
 */
public interface EventbusClientMethods {

	/**
	 * Connect to the mesh eventbus bridge via a websocket.
	 * 
	 * @param wsConnect
	 * @param failureHandler
	 */
	void eventbus(Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler);

	/**
	 * Connect to the mesh eventbus bridge via a websocket.
	 * 
	 * @param wsConnect
	 */
	default void eventbus(Handler<WebSocket> wsConnect) {
		eventbus(wsConnect, null);
	}
}
