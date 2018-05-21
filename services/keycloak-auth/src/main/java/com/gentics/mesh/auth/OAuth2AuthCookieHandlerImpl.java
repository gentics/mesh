package com.gentics.mesh.auth;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import javax.inject.Inject;

import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.auth.oauth2.impl.OAuth2TokenImpl;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler which will update and load the access_token within a dedicated cookie.
 */
public class OAuth2AuthCookieHandlerImpl implements OAuth2AuthCookieHandler {

	private static final Logger log = LoggerFactory.getLogger(OAuth2AuthCookieHandlerImpl.class);

	public static final String RC_AUTH_TOKEN_KEY = "access_token";

	/** The name for the cookie to store the access token in. */
	private static final String COOKIE_AUTH = "AUTH";

	/** The name for the cookie to store the refresh token in. */
	private static final String COOKIE_AUTH_REFRESH = "AUTH_REFRESH";

	/** The maximum age for the authorisation cookie. */
	private static final long DEFAULT_AUTH_TIMEOUT = 300;

	private OAuth2Auth auth;

	/**
	 * Create a new cookie handler.
	 * 
	 * @param auth
	 */
	@Inject
	public OAuth2AuthCookieHandlerImpl(OAuth2Auth auth) {
		this.auth = auth;
	}

	@Override
	public void handle(RoutingContext rc) {
		if (rc.user() == null) {
			if (rc.getCookie(COOKIE_AUTH) == null) {
				log.debug("No user information found, initiating authorisation");
			} else {
				setUser(rc);
			}
		} else {
			setAuthCookie(rc);
			AccessToken token = (AccessToken) rc.user();
			if (token.expired()) {
				log.info("Trying to refresh expired token");
				token.refresh(result -> refreshCallback(rc, result));
				return;
			}
		}

		log.debug("User authenticated");
		rc.next();

	}

	private void refreshCallback(RoutingContext rc, AsyncResult<Void> result) {
		if (result.failed()) {
			log.fatal("Refreshing access token failed", result.cause());
			rc.response().setStatusCode(INTERNAL_SERVER_ERROR.code()).end();
			return;
		}

		log.info("Token refresh successful");
		setAuthCookie(rc);
		rc.next();
		// internalHandle(rc);
	}

	/**
	 * Sets the authentication and the refresh cookie.
	 *
	 * @param rc
	 *            The current routing context
	 */
	private void setAuthCookie(RoutingContext rc) {
		JsonObject principal = rc.user().principal();

		Cookie cookie = Cookie.cookie(COOKIE_AUTH, principal.getString("access_token"))
			.setPath("/")
			.setMaxAge(principal.getLong("expires_in", DEFAULT_AUTH_TIMEOUT));

		rc.addCookie(cookie);

		cookie = Cookie.cookie(COOKIE_AUTH_REFRESH, principal.getString("refresh_token"))
			.setPath("/")
			.setMaxAge(principal.getLong("refresh_expires_in", DEFAULT_AUTH_TIMEOUT));

		rc.addCookie(cookie);
	}

	/**
	 * Loads the access token from the {@link #COOKIE_AUTH authorization cookie} and stores it in the routing context.
	 *
	 * @param rc
	 *            The current routing context
	 */
	private void setUser(RoutingContext rc) {
		if (rc.user() != null) {
			if (rc.get(RC_AUTH_TOKEN_KEY) == null) {
				rc.put(RC_AUTH_TOKEN_KEY, rc.user().principal().getString("access_token"));
			}
			return;
		}

		Cookie cookie = rc.getCookie(COOKIE_AUTH);

		if (log.isTraceEnabled()) {
			if (cookie == null) {
				log.trace("No authentication cookie set");
			}
		}

		JsonObject token = new JsonObject();

		// FIXME: This is quite ugly and definitely not in the spirit of OAuth2,
		// but if the portal should avoid "unnecessary" requests to Keycloak
		// something like this is unavoidable.
		token.put("access_token", rc.getCookie(COOKIE_AUTH).getValue());
		token.put("refresh_token", rc.getCookie(COOKIE_AUTH_REFRESH).getValue());

		User user = new OAuth2TokenImpl((OAuth2AuthProviderImpl) auth, token);

		rc.setUser(user);
		rc.put(RC_AUTH_TOKEN_KEY, user.principal().getString("access_token"));
	}

}
