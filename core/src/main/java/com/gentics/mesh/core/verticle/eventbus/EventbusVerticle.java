package com.gentics.mesh.core.verticle.eventbus;

import static com.gentics.mesh.core.verticle.eventbus.EventbusAddress.MESH_MIGRATION;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

@Component
@Scope("singleton")
@SpringVerticle
public class EventbusVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(EventbusVerticle.class);

	public EventbusVerticle() {
		super("eventbus");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addEventBusHandler();
	}

	private void addEventBusHandler() {
		SockJSHandlerOptions sockJSoptions = new SockJSHandlerOptions().setHeartbeatInterval(2000);
		SockJSHandler handler = SockJSHandler.create(vertx, sockJSoptions);
		BridgeOptions bridgeOptions = new BridgeOptions();
		bridgeOptions.addInboundPermitted(new PermittedOptions().setAddress(MESH_MIGRATION.toString()));
		bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddress(MESH_MIGRATION.toString()));
//		handler.bridge(bridgeOptions);
		handler.bridge(bridgeOptions, event -> {
			//			if (event.type() == BridgeEventType.SOCKET_CREATED) {
			//				log.info("A socket was created");
			//			}
			event.complete(true);
		});
		route("/*").handler(handler);

	}

}
