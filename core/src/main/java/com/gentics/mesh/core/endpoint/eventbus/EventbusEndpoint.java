package com.gentics.mesh.core.endpoint.eventbus;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.User;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

/**
 * The eventbus endpoint provides a SockJS compliant websocket eventbus bridge.
 */
public class EventbusEndpoint extends AbstractInternalEndpoint {

	private static final Logger log = LoggerFactory.getLogger(EventbusEndpoint.class);

	private final Vertx vertx;

	public EventbusEndpoint() {
		super("eventbus", null, null, null, null);
		this.vertx = null;
	}

	@Inject
	public EventbusEndpoint(Vertx vertx, MeshAuthChain chain, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super("eventbus", chain, localConfigApi, db, options);
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
		secureAll();
		InternalEndpointRoute endpoint = createRoute();
		endpoint.setRAMLPath("/");
		endpoint.description("This endpoint provides a sockjs compliant websocket which can be used to interface with the vert.x eventbus.");

		if (!isRamlGeneratorContext()) {
			SockJSHandlerOptions sockJSoptions = new SockJSHandlerOptions().setHeartbeatInterval(2000);
			SockJSHandler handler = SockJSHandler.create(vertx, sockJSoptions);
			SockJSBridgeOptions bridgeOptions = new SockJSBridgeOptions();
			for (MeshEvent event : MeshEvent.publicEvents()) {
				// TODO ensure that clients can't fire internal mesh events.
				bridgeOptions.addInboundPermitted(new PermittedOptions().setAddress(event.address));
				bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddress(event.address));
			}

			bridgeOptions.addInboundPermitted(new PermittedOptions().setAddressRegex("custom.*"));
			bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddressRegex("custom.*"));

			Router brigdeRoute = handler.bridge(bridgeOptions, event -> {
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

			endpoint.path("/*").subRouter(brigdeRoute);
		}
	}

	/**
	 * Returns whether the method is called from during the documentation generation context.
	 * @return
	 */
	private boolean isRamlGeneratorContext() {
		return localRouter == null;
	}
}
