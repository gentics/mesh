package com.gentics.mesh.core.verticle.eventbus;

import javax.inject.Inject;

import com.gentics.mesh.Events;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.rest.EndpointRoute;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

/**
 * The eventbus endpoint provides a SockJS compliant websocket eventbus bridge.
 */
public class EventbusEndpoint extends AbstractEndpoint {

	private static final Logger log = LoggerFactory.getLogger(EventbusEndpoint.class);

	@Inject
	public EventbusEndpoint() {
		super("eventbus");
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
			for (String addr : Events.publicEvents()) {
				bridgeOptions.addInboundPermitted(new PermittedOptions().setAddress(addr));
				bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddress(addr));
			}
			handler.bridge(bridgeOptions, event -> {
				if (log.isDebugEnabled()) {
					if (event.type() == BridgeEventType.SOCKET_CREATED) {
						log.debug("A websocket was created");
					}
				}
				// Only grant access to authenticated users
				User user = event.socket().webUser();
				boolean isAuthenticated = user != null;
				event.complete(isAuthenticated);
			});
		}

		secureAll();
		EndpointRoute endpoint = createEndpoint();
		endpoint.setRAMLPath("/");
		endpoint.description("This endpoint provides a sockjs complient websocket which can be used to interface with the vert.x eventbus.");
		endpoint.path("/*").handler(handler);

	}

}
