package com.gentics.mesh.core.verticle.eventbus;

import static com.gentics.mesh.Events.MESH_MIGRATION;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.EndpointRoute;

import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

/**
 * The eventbus endpoint provides a SockJS compliant websocket eventbus bridge.
 */
@Singleton
public class EventbusEndpoint extends AbstractEndpoint {

	@Inject
	public EventbusEndpoint(RouterStorage routerStorage) {
		super("eventbus", routerStorage);
	}

	public EventbusEndpoint() {
		super("eventbus", null);
	}

	public String getDescription() {
		return "This endpoint is a SockJS compliant websocket that creates a bridge to the mesh eventbus. It allows handling of various mesh specific events.";
	}

	@Override
	public void registerEndPoints() {
		addEventBusHandler();
	}

	private void addEventBusHandler() {
		SockJSHandler handler = null;
		if (localRouter != null) {
			SockJSHandlerOptions sockJSoptions = new SockJSHandlerOptions().setHeartbeatInterval(2000);
			handler = SockJSHandler.create(Mesh.vertx(), sockJSoptions);
			BridgeOptions bridgeOptions = new BridgeOptions();
			bridgeOptions.addInboundPermitted(new PermittedOptions().setAddress(MESH_MIGRATION));
			bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddress(MESH_MIGRATION));
			handler.bridge(bridgeOptions, event -> {
				// if (event.type() == BridgeEventType.SOCKET_CREATED) {
				// log.info("A socket was created");
				// }
				event.complete(true);
			});
		}

		EndpointRoute endpoint = createEndpoint();
		endpoint.setRAMLPath("/");
		endpoint.description("This endpoint provides a sockjs complient websocket which can be used to interface with the vert.x eventbus.");
		endpoint.path("/*").handler(handler);

	}

}
