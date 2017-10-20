package com.gentics.mesh.core.verticle.utility;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.rest.EndpointRoute;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Verticle providing endpoints for various utilities.
 */
@Singleton
public class UtilityEndpoint extends AbstractEndpoint {

	private UtilityHandler utilityHandler;

	@Inject
	public UtilityEndpoint(RouterStorage routerStorage, UtilityHandler utilityHandler) {
		super("utilities", routerStorage);
		this.utilityHandler = utilityHandler;
	}

	public UtilityEndpoint() {
		super("utilities", null);
	}

	@Override
	public String getDescription() {
		return "Provides endpoints for various utility actions";
	}

	@Override
	public void registerEndPoints() {
		secureAll();
		addResolveLinkHandler();
	}

	/**
	 * Add the handler for link resolving.
	 */
	private void addResolveLinkHandler() {
		EndpointRoute resolver = createEndpoint();
		resolver.path("/linkResolver");
		resolver.method(POST);
		resolver.description("Return the posted text and resolve and replace all found mesh links. "
				+ "A mesh link must be in the format {{mesh.link(\"UUID\",\"languageTag\")}}");
		resolver.addQueryParameters(NodeParametersImpl.class);
		resolver.exampleRequest("Some text before {{mesh.link(\"" + UUIDUtil.randomUUID() + "\", \"en\")}} and after.");
		resolver.exampleResponse(OK, "Some text before /api/v1/dummy/webroot/flower.jpg and after");
		resolver.handler(rc -> {
			utilityHandler.handleResolveLinks(rc);
		});
	}
}
