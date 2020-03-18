package com.gentics.mesh.distributed;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * The request delegator is responsible to delegate the request to the elected master instance.
 * The delegator is one of the core components of the coordination layer feature.
 */
public interface RequestDelegator extends Handler<RoutingContext> {

}
