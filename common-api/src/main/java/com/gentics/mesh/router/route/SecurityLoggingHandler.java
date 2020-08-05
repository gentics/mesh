package com.gentics.mesh.router.route;

import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class SecurityLoggingHandler implements Handler<RoutingContext> {

	public static final String SECURITY_LOGGER_CONTEXT_KEY = "securityLogger";
	public static final String SECURITY_LOGGER_NAME = "SecurityLogger";
	private static final Logger securityLogger = LoggerFactory.getLogger(SECURITY_LOGGER_NAME);

	private SecurityLoggingHandler() {
	}

	public static SecurityLoggingHandler create() {
		return new SecurityLoggingHandler();
	}

	@Override
	public void handle(RoutingContext context) {
		context.put(SECURITY_LOGGER_CONTEXT_KEY, new SecurityLogger(securityLogger, context));
		context.next();
	}
}
