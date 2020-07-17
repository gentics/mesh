package com.gentics.mesh.distributed;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * The request delegator is responsible to delegate the request to the elected master instance.
 * The delegator is one of the core components of the coordination layer feature.
 */
public interface RequestDelegator extends Handler<RoutingContext> {
	String MESH_FORWARDED_FROM_HEADER = "X-Mesh-Forwarded-From";

	/**
	 * Returns true if this instance can be written to.
	 * If not, mutating request should be delegated to the master.
	 *
	 * @return
	 */
	boolean canWrite();

	/**
	 * Delegates the request to the master instance.
	 * @param routingContext
	 */
	void redirectToMaster(RoutingContext routingContext);
}
