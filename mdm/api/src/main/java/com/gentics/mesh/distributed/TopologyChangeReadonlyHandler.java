package com.gentics.mesh.distributed;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * The TopologyChangeReadonlyHandler will let all mutation requests fail immediately, while the cluster topology is changing
 */
public interface TopologyChangeReadonlyHandler extends Handler<RoutingContext> {

}
