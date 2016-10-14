package com.gentics.mesh.auth;

import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;

/**
 * This class extends the Vert.x AuthHandler, so that it also works when the token is set as a cookie. Central authentication handler for mesh.
 */
@Singleton
public class MeshAuthHandler extends AuthHandlerImpl implements JWTAuthHandler {

	private static final Logger log = LoggerFactory.getLogger(JWTAuthHandlerImpl.class);

	private static final Pattern BEARER = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);

	private final JsonObject options;

	private MeshAuthProvider authProvider;

	@Inject
	public MeshAuthHandler(MeshAuthProvider authProvider) {
		super(authProvider);
		this.authProvider = authProvider;
		options = new JsonObject();
	}

	@Override
	public JWTAuthHandler setAudience(List<String> audience) {
		options.put("audience", new JsonArray(audience));
		return this;
	}

	@Override
	public JWTAuthHandler setIssuer(String issuer) {
		options.put("issuer", issuer);
		return this;
	}

	@Override
	public JWTAuthHandler setIgnoreExpiration(boolean ignoreExpiration) {
		options.put("ignoreExpiration", ignoreExpiration);
		return this;
	}

	@Override
	public void handle(RoutingContext context) {
		Cookie tokenCookie = context.getCookie(MeshAuthProvider.TOKEN_COOKIE_KEY);
		if (tokenCookie != null) {
			context.request().headers().set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCookie.getValue());
		}

		User user = context.user();
		if (user != null) {
			// Already authenticated in, just authorise
			authorise(user, context);
		} else {
			final HttpServerRequest request = context.request();

			String token = null;

			if (request.method() == HttpMethod.OPTIONS && request.headers().get(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS) != null) {
				for (String ctrlReq : request.headers().get(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS).split(",")) {
					if (ctrlReq.equalsIgnoreCase("authorization")) {
						// this request has auth in access control
						context.next();
						return;
					}
				}
			}

			final String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);

			if (authorization != null) {
				String[] parts = authorization.split(" ");
				if (parts.length == 2) {
					final String scheme = parts[0], credentials = parts[1];

					if (BEARER.matcher(scheme).matches()) {
						token = credentials;
					}
				} else {
					log.warn("Format is Authorization: Bearer [token]");
					context.fail(401);
					return;
				}
			} else {
				log.warn("No Authorization header was found");
				context.fail(401);
				return;
			}

			JsonObject authInfo = new JsonObject().put("jwt", token).put("options", options);
			authProvider.authenticate(authInfo, res -> {

				// Authentication was successful. Lets update the token cookie to keep it alive
				if (res.succeeded()) {
					final User user2 = res.result();
					context.setUser(user2);
					String jwtToken = authProvider.generateToken(user2);
					// Remove the original cookie and set the new one
					context.removeCookie(MeshAuthProvider.TOKEN_COOKIE_KEY);
					context.addCookie(Cookie.cookie(MeshAuthProvider.TOKEN_COOKIE_KEY, jwtToken)
							.setMaxAge(Mesh.mesh().getOptions().getAuthenticationOptions().getTokenExpirationTime()));
					authorise(user2, context);
				} else {
					log.warn("JWT decode failure", res.cause());
					context.fail(401);
				}
			});
		}
	}

}
