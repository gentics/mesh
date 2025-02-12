package com.gentics.mesh.router.route;

import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for the {@link SecurityLogger}
 */
public class SecurityLoggingHandler implements Handler<RoutingContext> {

	public static final String SECURITY_LOGGER_CONTEXT_KEY = "securityLogger";
	public static final String SECURITY_LOGGER_NAME = "SecurityLogger";
	private static final Logger securityLogger = LoggerFactory.getLogger(SECURITY_LOGGER_NAME);

	private SecurityLoggingHandler() {
	}

	/**
	 * Create a new security logger handler.
	 * 
	 * @return
	 */
	public static SecurityLoggingHandler create() {
		return new SecurityLoggingHandler();
	}

	@Override
	public void handle(RoutingContext context) {
		context.put(SECURITY_LOGGER_CONTEXT_KEY, new SecurityLogger(securityLogger, context));
		context.next();
	}
}
