package com.gentics.mesh.core.verticle.utility;

import static io.vertx.core.http.HttpMethod.POST;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.rest.Endpoint;

/**
 * Verticle providing endpoints for various utilities.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class UtilityVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private UtilityHandler utilityHandler;

	public UtilityVerticle() {
		super("utilities");
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
