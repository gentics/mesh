package com.gentics.mesh.core.verticle.eventbus;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

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
		SockJSHandler handler = SockJSHandler.create(vertx);
		BridgeOptions options = new BridgeOptions();
		options.addInboundPermitted(new PermittedOptions().setAddress("mesh.schema.migration"));
		options.addOutboundPermitted(new PermittedOptions().setAddress("mesh.schema.migration"));
		handler.bridge(options, event -> {
			if (event.type() == BridgeEventType.SOCKET_CREATED) {
				log.info("A socket was created");
			}
			event.complete(true);
		});
		route("/*").handler(handler);

	}

}
