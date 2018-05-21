package com.gentics.mesh.auth;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Mesh OAuth Service which controls all oauth specific tasks.
 */
public interface MeshOAuthService extends Handler<RoutingContext> {

}
