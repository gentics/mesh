package com.gentics.mesh.router.route;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler which adds the X-Powered-By header.
 */
public class PoweredByHandler implements Handler<RoutingContext> {

	/**
	 * Create a new powered by handler.
	 * 
	 * @return
	 */
	public static PoweredByHandler create() {
		return new PoweredByHandler();
	}

	@Override
	public void handle(RoutingContext rc) {
		rc.response().putHeader("X-Powered-By", "getmesh.io");
		rc.next();
	}

}
