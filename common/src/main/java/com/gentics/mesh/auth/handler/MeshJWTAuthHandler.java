
package com.gentics.mesh.auth.handler;

import static io.vertx.core.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.auth.AuthenticationResult;
import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.shared.SharedKeys;

import io.vertx.core.Future;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;
import io.vertx.ext.web.impl.UserContextInternal;

/**
 * This class extends the Vert.x AuthenticationHandlerImpl, so that it also works when the token is set as a cookie.
 * 
 * Central authentication handler for mesh. All requests to secured resources must pass this handler.
 */
@Singleton
public class MeshJWTAuthHandler extends AuthenticationHandlerImpl<MeshJWTAuthProvider> implements JWTAuthHandler, MeshAuthHandler {

	private static final Logger log = LoggerFactory.getLogger(MeshJWTAuthHandler.class);

	private static final Pattern BEARER = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);

	public static final String ANONYMOUS_USERNAME = "anonymous";

	private final JsonObject options;

	private final MeshJWTAuthProvider authProvider;

	private final MeshOptions meshOptions;

	@Inject
	public MeshJWTAuthHandler(MeshJWTAuthProvider authProvider, MeshOptions meshOptions) {
		super(authProvider);
		this.authProvider = authProvider;
		this.meshOptions = meshOptions;

		options = new JsonObject();
	}

	@Override
	public void handle(RoutingContext context) {
		handle(context, false);
	}

	/**
	 * Handle the JWT authentication. No authentication will be performed when the user has already been added to the context.
	 * 
	 * @param context
	 * @param ignoreDecodeErrors
	 */
	public void handle(RoutingContext context, boolean ignoreDecodeErrors) {

		// 1. Check whether the user is already authenticated
		User user = context.user();
		if (user != null) {
			context.next();
			return;
		}
		handleJWTAuth(context, ignoreDecodeErrors);
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
		Cookie tokenCookie = context.request().getCookie(SharedKeys.TOKEN_COOKIE_KEY);
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
		Credentials authInfo = new TokenCredentials(token);
		authProvider.authenticateJWT(authInfo, res -> {

			// Authentication was successful.
			if (res.succeeded()) {
				AuthenticationResult result = res.result();
				User authenticatedUser = result.getUser();
				((UserContextInternal) context.userContext()).setUser(authenticatedUser);

				// Lets update the token cookie if this is request is using a regular token (e.g. not an api token) to keep it alive
				if (!result.isUsingAPIKey()) {
					String jwtToken = authProvider.generateToken(authenticatedUser);
					// Remove the original cookie and set the new one
					context.response().removeCookie(SharedKeys.TOKEN_COOKIE_KEY);
					context.response().addCookie(Cookie.cookie(SharedKeys.TOKEN_COOKIE_KEY, jwtToken)
						.setHttpOnly(true)
						.setMaxAge(meshOptions.getAuthenticationOptions().getTokenExpirationTime())
						.setPath("/"));
				}
				context.next();
			} else {
				if (res.cause() != null) {
					if (log.isDebugEnabled()) {
						log.error("Authentication failed in Mesh JWT handler.", res.cause());
					}
				}
				if (ignoreDecodeErrors) {
					context.next();
				} else {
					log.warn("JWT decode failure", res.cause());
					handle401(context);
				}
			}
		});
	}

	@Override
	public JWTAuthHandler scopeDelimiter(String s) {
		return this;
	}

	@Override
	public JWTAuthHandler withScope(String s) {
		return this;
	}

	@Override
	public JWTAuthHandler withScopes(List<String> list) {
		return this;
	}

	@Override
	public Future<User> authenticate(RoutingContext context) {
		return Future.succeededFuture(context.user());
	}
}
