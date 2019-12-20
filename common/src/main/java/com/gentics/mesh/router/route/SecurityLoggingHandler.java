package com.gentics.mesh.router.route;

import static io.vertx.core.logging.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Vertx;

@Singleton
public class SecurityLoggingHandler implements Handler<RoutingContext> {

	public static final String SECURITY_LOGGER_CONTEXT_KEY = "securityLogger";
	public static final String SECURITY_LOGGER_NAME = "SecurityLogger";
	private static final Logger securityLogger = getLogger(SECURITY_LOGGER_NAME);
	private static final Logger log = getLogger(SecurityLoggingHandler.class);

	private final Vertx vertx;

	@Inject
	public SecurityLoggingHandler(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public void handle(RoutingContext context) {
		SecurityLogger.forCurrentUser(vertx, securityLogger, context)
			.subscribe(logger -> {
				context.put(SECURITY_LOGGER_CONTEXT_KEY, logger);
				context.next();
			}, err -> {
				log.error("Could not create security logger", err);
				context.next();
			});
	}
}
