package com.gentics.mesh.auth;

import java.util.Base64;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;

/**
 * This class is a modification of Vertx'
 * {@link io.vertx.ext.web.handler.impl.BasicAuthHandlerImpl} The only
 * difference is that if the user is not authenticated (401), no response header
 * is set. This prevents the ugly popup in browsers asking for credentials.
 * 
 * @author philippguertler
 */
public class MeshBasicAuthHandler extends AuthHandlerImpl {

	public MeshBasicAuthHandler(AuthProvider authProvider) {
		super(authProvider);
	}

	public static MeshBasicAuthHandler create(AuthProvider authProvider) {
		return new MeshBasicAuthHandler(authProvider);
	}

	@Override
	public void handle(RoutingContext context) {
		User user = context.user();
		if (user != null) {
			// Already authenticated in, just authorise
			authorise(user, context);
		} else {
			HttpServerRequest request = context.request();
			String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);

			if (authorization == null) {
				handle401(context);
			} else {
				String suser;
				String spass;
				String sscheme;

				try {
					String[] parts = authorization.split(" ");
					sscheme = parts[0];
					String[] credentials = new String(Base64.getDecoder().decode(parts[1])).split(":");
					suser = credentials[0];
					// when the header is: "user:"
					spass = credentials.length > 1 ? credentials[1] : null;
				} catch (ArrayIndexOutOfBoundsException e) {
					handle401(context);
					return;
				} catch (IllegalArgumentException | NullPointerException e) {
					// IllegalArgumentException includes PatternSyntaxException
					context.fail(e);
					return;
				}

				if (!"Basic".equals(sscheme)) {
					context.fail(400);
				} else {
					JsonObject authInfo = new JsonObject().put("username", suser).put("password", spass);
					authProvider.authenticate(authInfo, res -> {
						if (res.succeeded()) {
							User authenticated = res.result();
							context.setUser(authenticated);
							authorise(authenticated, context);
						} else {
							handle401(context);
						}
					});
				}
			}
		}
	}

	private void handle401(RoutingContext context) {
		context.fail(401);
	}
}
