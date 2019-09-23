package com.gentics.mesh.auth;

import static io.vertx.core.http.HttpHeaders.AUTHORIZATION;

import java.util.HashSet;
import java.util.Set;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.jwt.impl.JWTAuthProviderImpl;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import io.vertx.ext.web.handler.impl.HttpStatusException;

public class MeshOAuth2AuthHandlerImpl extends AuthHandlerImpl {

	public static final HttpStatusException FORBIDDEN = new HttpStatusException(403);
	public static final HttpStatusException UNAUTHORIZED = new HttpStatusException(401);
	public static final HttpStatusException BAD_REQUEST = new HttpStatusException(400);

	enum Type {
		BEARER("Bearer");

		private final String label;

		Type(String label) {
			this.label = label;
		}

		public boolean is(String other) {
			return label.equalsIgnoreCase(other);
		}
	}

	private final Set<String> scopes = new HashSet<>();

	protected final Type type;

	public MeshOAuth2AuthHandlerImpl(AuthProvider authProvider) {
		super(verifyProvider(authProvider));
		this.type = Type.BEARER;
	}

	protected final void parseAuthorization(RoutingContext ctx, boolean optional, Handler<AsyncResult<String>> handler) {

		final HttpServerRequest request = ctx.request();
		final String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);

		if (authorization == null) {
			if (optional) {
				// this is allowed
				handler.handle(Future.succeededFuture());
			} else {
				handler.handle(Future.failedFuture(UNAUTHORIZED));
			}
			return;
		}

		try {
			int idx = authorization.indexOf(' ');

			if (idx <= 0) {
				handler.handle(Future.failedFuture(BAD_REQUEST));
				return;
			}

			if (!type.is(authorization.substring(0, idx))) {
				handler.handle(Future.failedFuture(UNAUTHORIZED));
				return;
			}

			handler.handle(Future.succeededFuture(authorization.substring(idx + 1)));
		} catch (RuntimeException e) {
			handler.handle(Future.failedFuture(e));
		}
	}

	/**
	 * This is a verification step, it can abort the instantiation by throwing a RuntimeException
	 *
	 * @param provider
	 *            a auth provider
	 * @return the provider if valid
	 */
	private static AuthProvider verifyProvider(AuthProvider provider) {
		return provider;
	}

	@Override
	public AuthHandler addAuthority(String authority) {
		scopes.add(authority);
		return this;
	}

	@Override
	public AuthHandler addAuthorities(Set<String> authorities) {
		this.scopes.addAll(authorities);
		return this;
	}

	@Override
	public void handle(RoutingContext rc) {
		// Don't run the OAuth2 handler if a user has already been authenticated.
		if (rc.user() != null) {
			rc.next();
			return;
		}

		// No need to bother the oauth2 handler if no token info was provided.
		// Maybe the anonymous handler can process this.
		final HttpServerRequest request = rc.request();
		final String authorization = request.headers().get(AUTHORIZATION);
		boolean hasAuth = authorization != null;
		if (!hasAuth) {
			rc.next();
			return;
		}

		super.handle(rc);
	}

	@Override
	public void parseCredentials(RoutingContext context, Handler<AsyncResult<JsonObject>> handler) {
		parseAuthorization(context, true, parseAuthorization -> {
			if (parseAuthorization.failed()) {
				handler.handle(Future.failedFuture(parseAuthorization.cause()));
				return;
			}
			// Authorization header could be null as we mark it as optional
			final String token = parseAuthorization.result();

			if (token == null) {
				context.next();
			} else {
//				// attempt to decode the token and handle it as a user
//				((JWTAuthProviderImpl) authProvider).decodeToken(token, decodeToken -> {
//					if (decodeToken.failed()) {
//						handler.handle(Future.failedFuture(new HttpStatusException(401, decodeToken.cause().getMessage())));
//						return;
//					}
//
//					context.setUser(decodeToken.result());
//					// continue
//					handler.handle(Future.succeededFuture());
//				});
			}
		});
	}

}