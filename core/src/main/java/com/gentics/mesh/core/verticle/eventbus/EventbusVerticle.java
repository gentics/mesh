package com.gentics.mesh.core.verticle.eventbus;

import static com.gentics.mesh.core.verticle.eventbus.EventbusAddress.MESH_MIGRATION;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.Endpoint;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

public class EventbusVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(EventbusVerticle.class);

	public EventbusVerticle(RouterStorage routerStorage, MeshSpringConfiguration springConfig) {
		super("eventbus", routerStorage, springConfig);
	}

	public String getDescription() {
		return "This endpoint is a SockJS compliant websocket that creates a bridge to the mesh eventbus. It allows handling of various mesh specific events.";
	}

	@Override
	public void registerEndPoints() throws Exception {
		addEventBusHandler();
	}

	private void addEventBusHandler() {
		SockJSHandler handler = null;
		if (localRouter != null) {
			SockJSHandlerOptions sockJSoptions = new SockJSHandlerOptions().setHeartbeatInterval(2000);
			handler = SockJSHandler.create(vertx, sockJSoptions);
			BridgeOptions bridgeOptions = new BridgeOptions();
			bridgeOptions.addInboundPermitted(new PermittedOptions().setAddress(MESH_MIGRATION.toString()));
			bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddress(MESH_MIGRATION.toString()));
			//		handler.bridge(bridgeOptions);
			handler.bridge(bridgeOptions, event -> {
				//	if (event.type() == BridgeEventType.SOCKET_CREATED) {
				//		log.info("A socket was created");
				//	}
				event.complete(true);
			});
		}

		Endpoint endpoint = createEndpoint();
		endpoint.setRAMLPath("/");
		endpoint.description("This endpoint provides a sockjs complient websocket which can be used to interface with the vert.x eventbus.");
		endpoint.path("/*").handler(handler);

	}

}
