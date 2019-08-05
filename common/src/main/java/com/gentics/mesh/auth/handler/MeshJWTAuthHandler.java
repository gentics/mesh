package com.gentics.mesh.auth.handler;

import static io.vertx.core.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.AuthenticationResult;
import com.gentics.mesh.auth.MeshOAuthService;
import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.cli.BootstrapInitializer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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
 * This class extends the Vert.x AuthHandler, so that it also works when the token is set as a cookie.
 * 
 * Central authentication handler for mesh. All requests to secured resources must pass this handler.
 */
@Singleton
public class MeshJWTAuthHandler extends AuthHandlerImpl implements JWTAuthHandler, MeshAuthHandler {

	private static final Logger log = LoggerFactory.getLogger(JWTAuthHandlerImpl.class);

	private static final Pattern BEARER = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);

	public static final String ANONYMOUS_USERNAME = "anonymous";

	private final JsonObject options;

	private MeshJWTAuthProvider authProvider;

	private BootstrapInitializer boot;

	@Inject
	public MeshJWTAuthHandler(MeshJWTAuthProvider authProvider, MeshOAuthService oauthService, BootstrapInitializer boot) {
		super(authProvider);
		this.authProvider = authProvider;
		this.boot = boot;

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
		handle(context, false);
	}

	public void handle(RoutingContext context, boolean ignoreDecodeErrors) {

		// 1. Check whether the user is already authenticated
		User user = context.user();
		if (user != null) {
			context.next();
			return;
		}
		handleJWTAuth(context, ignoreDecodeErrors);
	}

	@Override
	public void parseCredentials(RoutingContext arg0, Handler<AsyncResult<JsonObject>> arg1) {
		// Not needed for this handler
	}

	/**
	 * Handle the JWT authentication part.
	 * 
	 * @param context
	 * @param ignoreDecodeErrors
	 */
	private void handleJWTAuth(RoutingContext context, boolean ignoreDecodeErrors) {

		// Don't do anything if the user has already been authenticated by a previous handler
		if (context.user() != null) {
			context.next();
			return;
		}

		// Mesh accepts JWT tokens via the cookie as well in order to handle JWT even for regular HTTP Download requests (eg. non ajax requests (static file
		// downloads)).
		// Store the found token value into the authentication header value. This will effectively overwrite the AUTHORIZATION header value.
		Cookie tokenCookie = context.getCookie(MeshJWTAuthProvider.TOKEN_COOKIE_KEY);
		if (tokenCookie != null) {
			context.request().headers().set(AUTHORIZATION, "Bearer " + tokenCookie.getValue());
		}

		final HttpServerRequest request = context.request();
		String token = null;

		// Try to load the token from the AUTHORIZATION header value
		final String authorization = request.headers().get(AUTHORIZATION);
		if (authorization != null) {
			String[] parts = authorization.split(" ");
			if (parts.length == 2) {
				final String scheme = parts[0], credentials = parts[1];

				if (BEARER.matcher(scheme).matches()) {
					token = credentials;
				}
			} else {
				log.warn("Format is Authorization: Bearer [token]");
				handle401(context);
				return;
			}
		} else {
			// Continue and let the next handler deal with this situation
			context.next();
			return;
		}

		// Check whether an actual token value was found otherwise we can exit early
		if (token == null) {
			log.warn("No Authorization token value was found");
			handle401(context);
			return;
		}

		// 4. Authenticate the found token using JWT
		JsonObject authInfo = new JsonObject().put("jwt", token).put("options", options);
		authProvider.authenticateJWT(authInfo, res -> {

			// Authentication was successful.
			if (res.succeeded()) {
				AuthenticationResult result = res.result();
				User authenticatedUser = result.getUser();
				context.setUser(authenticatedUser);

				// Lets update the token cookie if this is request is using a regular token (e.g. not an api token) to keep it alive
				if (!result.isUsingAPIKey()) {
					String jwtToken = authProvider.generateToken(authenticatedUser);
					// Remove the original cookie and set the new one
					context.removeCookie(MeshJWTAuthProvider.TOKEN_COOKIE_KEY);
					context.addCookie(Cookie.cookie(MeshJWTAuthProvider.TOKEN_COOKIE_KEY, jwtToken)
						.setMaxAge(boot.mesh().getOptions().getAuthenticationOptions().getTokenExpirationTime()).setPath("/"));
				}
				authorizeUser(authenticatedUser, context);
				return;
			} else {
				if (ignoreDecodeErrors) {
					context.next();
				} else {
					log.warn("JWT decode failure", res.cause());
					handle401(context);
					return;
				}
			}
		});
	}

}
