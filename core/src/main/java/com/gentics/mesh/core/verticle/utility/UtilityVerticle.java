package com.gentics.mesh.core.verticle.utility;

import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.Endpoint;

/**
 * Verticle providing endpoints for various utilities.
 */
@Singleton
public class UtilityVerticle extends AbstractWebVerticle {

	private UtilityHandler utilityHandler;

	@Inject
	public UtilityVerticle(RouterStorage routerStorage, UtilityHandler utilityHandler) {
		super("utilities", routerStorage);
		this.utilityHandler = utilityHandler;
	}

	public UtilityVerticle() {
		super("utilities", null);
	}

	@Override
	public String getDescription() {
		return "Provides endpoints for various utility actions";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		addResolveLinkHandler();
	}

	/**
	 * Add the handler for link resolving
	 */
	private void addResolveLinkHandler() {
		Endpoint resolver = createEndpoint();
		resolver.path("/linkResolver");
		resolver.method(POST);
		resolver.description("Return the posted text and resolve and replace all found links.");
		resolver.handler(rc -> {
			utilityHandler.handleResolveLinks(rc);
		});
	}
}
