package com.gentics.mesh.core.endpoint.eventbus;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

import io.vertx.core.Vertx;
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
public class EventbusEndpoint extends AbstractInternalEndpoint {

	private static final Logger log = LoggerFactory.getLogger(EventbusEndpoint.class);

	private final Vertx vertx;

	public EventbusEndpoint() {
		super("eventbus", null);
		this.vertx = null;
	}

	@Inject
	public EventbusEndpoint(Vertx vertx, MeshAuthChain chain) {
		super("eventbus", chain);
		this.vertx = vertx;
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
			handler = SockJSHandler.create(vertx, sockJSoptions);
			BridgeOptions bridgeOptions = new BridgeOptions();
			for (MeshEvent event : MeshEvent.publicEvents()) {
				bridgeOptions.addInboundPermitted(new PermittedOptions().setAddress(event.address));
				bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddress(event.address));
			}

			bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex("custom.*"));
			bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddressRegex("custom.*"));

			handler.bridge(bridgeOptions, event -> {
				if (log.isDebugEnabled()) {
					if (event.type() == BridgeEventType.SOCKET_CREATED) {
						// TODO maybe it would be useful to send a reply to the user.
						// This way the user knows when mesh is ready to relay events.
						// Use REGISTER for those cases.
						log.debug("A websocket was created");
					}
				}
				// Only grant access to authenticated users
				User user = event.socket().webUser();
				boolean isAuthenticated = user != null;
				log.debug("Eventbridge creation. User was authenticated: " + isAuthenticated);
				event.complete(isAuthenticated);
			});
		}

		secureAll();
		InternalEndpointRoute endpoint = createRoute();
		endpoint.setRAMLPath("/");
		endpoint.description("This endpoint provides a sockjs complient websocket which can be used to interface with the vert.x eventbus.");
		endpoint.path("/*").handler(handler);

	}

}
