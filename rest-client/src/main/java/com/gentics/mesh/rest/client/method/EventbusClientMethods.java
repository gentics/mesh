package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.rest.client.MeshWebsocket;

/**
 * Eventbus endpoint specific REST methods.
 */
public interface EventbusClientMethods {

	/**
	 * Connect to the mesh eventbus bridge via a websocket.
	 * @return
	 */
	MeshWebsocket eventbus();
}
