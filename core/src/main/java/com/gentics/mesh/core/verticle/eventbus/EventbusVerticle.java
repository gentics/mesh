package com.gentics.mesh.core.verticle.eventbus;

import static com.gentics.mesh.core.verticle.eventbus.EventbusAddress.MESH_MIGRATION;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.Endpoint;

import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

@Singleton
public class EventbusVerticle extends AbstractWebVerticle {

	@Inject
	public EventbusVerticle(RouterStorage routerStorage) {
		super("eventbus", routerStorage);
	}

	public EventbusVerticle() {
		super("eventbus", null);
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
			// handler.bridge(bridgeOptions);
			handler.bridge(bridgeOptions, event -> {
				// if (event.type() == BridgeEventType.SOCKET_CREATED) {
				// log.info("A socket was created");
				// }
				event.complete(true);
			});
		}

		Endpoint endpoint = createEndpoint();
		endpoint.setRAMLPath("/");
		endpoint.description("This endpoint provides a sockjs complient websocket which can be used to interface with the vert.x eventbus.");
		endpoint.path("/*").handler(handler);

	}

}
