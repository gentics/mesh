package com.gentics.mesh.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;

/**
 * A wrapper around {@link JWTAuthHandlerImpl}, allowing proceeding with the service for the selected JWT errors. The errors are logged as well.
 */
public class MeshOAuthJWTHandler extends JWTAuthHandlerImpl {

	private static final Logger log = LoggerFactory.getLogger(MeshOAuthJWTHandler.class);

	public MeshOAuthJWTHandler(JWTAuth authProvider) {
		super(authProvider, null);
	}

	@Override
	protected void processException(RoutingContext ctx, Throwable exception) {
		if (exception != null) {
			if (exception instanceof HttpException) {
				final int statusCode = ((HttpException) exception).getStatusCode();
				final String payload = ((HttpException) exception).getPayload();

				switch (statusCode) {
				case 302:
					ctx.response().putHeader(HttpHeaders.LOCATION, payload).setStatusCode(302)
							.end("Redirecting to " + payload + ".");
					return;
				default:
					log.error("HTTP Error at OAuth JWT handler", exception);
					if (statusCode > 399 && statusCode < 500) {
						ctx.next();
					} else {
						ctx.fail(statusCode, exception);
					}
					return;
				}
			}
		}
		
		// fallback 500+
		log.error("Error at OAuth JWT handler", exception);
		ctx.fail(exception);
	}
}
