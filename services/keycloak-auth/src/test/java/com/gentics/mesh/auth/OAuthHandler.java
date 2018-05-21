package com.gentics.mesh.auth;
//package com.gentics.mesh.auth.keycloak;
//
//import static com.gentics.mesh.portal.api.PortalFlags.RC_IS_PREVIEW;
//import static com.gentics.mesh.portal.api.PortalFlags.RC_NODE_TO_RENDER;
//import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
//import static io.netty.handler.codec.http.HttpResponseStatus.SEE_OTHER;
//import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
//
//import java.util.HashMap;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//
//import com.gentics.mesh.etc.config.AuthenticationOptions;
//import com.gentics.mesh.portal.api.PortalFlags;
//import com.gentics.mesh.portal.api.RouteOrder;
//import com.gentics.mesh.portal.authentication.AuthorisationCheckHandler;
//import com.gentics.mesh.portal.config.PortalConfig;
//import com.gentics.mesh.portal.config.PortalConfigLoader;
//import com.gentics.mesh.portal.config.option.EndpointOptions;
//
//import io.vertx.core.AsyncResult;
//import io.vertx.core.Handler;
//import io.vertx.core.Vertx;
//import io.vertx.core.http.HttpClientOptions;
//import io.vertx.core.http.HttpHeaders;
//import io.vertx.core.json.DecodeException;
//import io.vertx.core.json.JsonObject;
//import io.vertx.core.logging.Logger;
//import io.vertx.core.logging.LoggerFactory;
//import io.vertx.ext.auth.User;
//import io.vertx.ext.auth.oauth2.AccessToken;
//import io.vertx.ext.auth.oauth2.OAuth2Auth;
//import io.vertx.ext.auth.oauth2.impl.AccessTokenImpl;
//import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
//import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
//import io.vertx.ext.jwt.JWT;
//import io.vertx.ext.web.Cookie;
//import io.vertx.ext.web.Router;
//import io.vertx.ext.web.RoutingContext;
//import io.vertx.ext.web.handler.OAuth2AuthHandler;
//
//public class OAuthHandler implements Handler<RoutingContext> {
//
//	/** Field in the loaded Mesh node containing authorization requirements. */
//	public static final String RC_AUTH_FIELD_KEY = "auth";
//	/** The routing context key for the access token. */
//	public static final String RC_AUTH_TOKEN_KEY = "access_token";
//
//	/**
//	 * A flag in the routing context indicating whether authorisation has already been handled for the current request. Since authorisation can be "requested"
//	 * by the loaded node itself, it's possible that redirects or reroutings happen after the node has been loaded.
//	 */
//	private static final String RC_AUTH_DONE_KEY = "auth_done";
//	/**
//	 * A flag in the routing context indicating whether routing for the current request should be restarted. This can be necessary when we need to start the
//	 * OAuth2 process <em>after</em> a Mesh node is already loaded.
//	 */
//	private static final String RC_AUTH_REROUTE_KEY = "auth_reroute";
//	/** The name for the cookie to store the access token in. */
//	private static final String COOKIE_AUTH = "AUTH";
//	/** The name for the cookie to store the refresh token in. */
//	private static final String COOKIE_AUTH_REFRESH = "AUTH_REFRESH";
//	/** The name for the cookie to store the URL to redirect to after the OAuth2 process. */
//	private static final String COOKIE_AUTH_REDIRECT = "AUTH_REDIRECT";
//	/** Default redirection path, when neither the respective cookie nor a <code>Referer</code> header are present. */
//	private static final String DEFAULT_REDIRECT = "/";
//	/** The maximum age for the authorisation cookie. */
//	private static final long DEFAULT_AUTH_TIMEOUT = 300;
//
//	private final Logger log = LoggerFactory.getLogger(OAuthHandler.class);
//	/** Protected endpoints from the configuration. */
//	private final Map<String, EndpointOptions> staticEndpoints = new HashMap<>();
//	/** Protected endpoints defined in the Mesh nodes themselves. */
//	private final Map<String, EndpointOptions> temporaryEndpoints;
//	/** The endpoint that will trigger the OAuth2 process. */
//	private final String checkEndpoint;
//	/** Currently just the Keycloak configuration. */
//	private final JsonObject extraConfig;
//	private final OAuth2Auth provider;
//	private final boolean disabled;
//
//	private PortalConfig portalConfig;
//
//	/**
//	 * Default constructor.
//	 *
//	 * Checks whether authentication should be enabled and creates the OAuth2 authentication provider.
//	 *
//	 * @param vertx
//	 *            The Vertx instance to use
//	 * @param portalConfig
//	 *            The portal configuration
//	 */
//	public OAuthHandler(Vertx vertx, PortalConfig portalConfig) {
//		this.portalConfig = portalConfig;
//		AuthenticationOptions authOptions = portalConfig.getAuthOptions();
//		boolean disable = false;
//
//		if (authOptions.disabled()) {
//			log.info("No login or protected endpoints configured");
//			log.info("Authentication disabled");
//
//			disable = true;
//		}
//
//		JsonObject keycloakConfig = PortalConfigLoader.loadKeycloakOptions();
//
//		if (keycloakConfig.isEmpty()) {
//			log.info("Authentication disabled");
//
//			disable = true;
//		}
//
//		if (disable) {
//			temporaryEndpoints = null;
//			checkEndpoint = null;
//			extraConfig = null;
//			provider = null;
//			disabled = true;
//
//			return;
//		}
//
//		HttpClientOptions httpClientOptions = new HttpClientOptions();
//
//		if (authOptions.isInsecureSsl()) {
//			log.warn("Accepting insecure SSL connections is enabled in the configuration");
//			log.warn("This should not be done on production environments");
//
//			httpClientOptions.setTrustAll(true).setVerifyHost(false);
//		}
//
//		temporaryEndpoints = new LinkedHashMap<String, EndpointOptions>() {
//			private static final long serialVersionUID = 7570880730625186249L;
//
//			@Override
//			protected boolean removeEldestEntry(Map.Entry<String, EndpointOptions> entry) {
//				return size() >= authOptions.getMaxTemporaryEntries();
//			}
//		};
//
//		checkEndpoint = authOptions.getCheckEndpoint();
//		provider = KeycloakAuth.create(vertx, keycloakConfig, httpClientOptions);
//		extraConfig = keycloakConfig;
//		disabled = false;
//	}
//
//	/**
//	 * Sets up the OAuth2 handler for the {@link AuthenticationOptions#getCheckEndpoint() check endpoint} as well as authentication for the
//	 * {@link AuthenticationOptions#getLoginEndpoint() login endpoint} and the configured {@link AuthenticationOptions#getEndPoints() protected endpoints}.
//	 *
//	 * @param router
//	 *            The router to add handlers to
//	 */
//	public void addStaticRoutes(Router router) {
//		if (disabled) {
//			log.debug("Not adding routes because authentication is disabled");
//			return;
//		}
//
//
//		AuthenticationOptions authOptions = portalConfig.getAuthOptions();
//
//		String path = authOptions.getCallbackEndpoint();
//		String callback = "http://" + authOptions.getCallbackHost() + ":" + portalConfig.getServerPort() + path;
//		OAuth2AuthHandler oauth = OAuth2AuthHandler.create(provider, callback);
//
//		log.info("Setting up OAuth2 handler with callback {}", callback);
//
//		oauth.setupCallback(router.get(path));
//
//		path = authOptions.getLoginEndpoint();
//
//		if (path != null && !path.isEmpty()) {
//			router.route(path).order(RouteOrder.API_ROUTES.getOrder()).handler(rc -> {
//				prepareRedirect(rc, rc.request().getHeader(HttpHeaders.REFERER));
//				rc.next();
//			});
//			router.route(path).order(RouteOrder.API_ROUTES.getOrder()).handler(oauth);
//			router.route(path).order(RouteOrder.API_ROUTES.getOrder()).handler(this::handlePostAuth).failureHandler(rc -> {
//				log.error("Error handling request {}", rc.failure(), rc.request().absoluteURI());
//				rc.response().setStatusCode(INTERNAL_SERVER_ERROR.code()).end();
//			});
//
//			log.debug("Login endpoint configured: {}", path);
//		}
//
//		path = checkEndpoint;
//		router.route(path).order(RouteOrder.API_ROUTES.getOrder()).handler(oauth);
//		router.route(path).order(RouteOrder.API_ROUTES.getOrder()).handler(this::handlePostAuth);
//
//		for (EndpointOptions endpoint : authOptions.getEndPoints()) {
//			if (addEndpoint(endpoint)) {
//				path = endpoint.getPath();
//				router.route(path).order(RouteOrder.API_ROUTES.getOrder()).handler(this);
//			}
//		}
//	}
//
//	/**
//	 * Checks if the {@link PortalFlags#RC_NODE_TO_RENDER node to render} has authentication settings, and initiates authentication if necessary.
//	 *
//	 * <p>
//	 * When the current request has {@link PortalFlags#RC_IS_PREVIEW} or {@link #RC_AUTH_DONE_KEY} set in the routing context there is nothing more to be done.
//	 * </p>
//	 *
//	 * <p>
//	 * When the currently loaded Mesh node has a field {@link #RC_AUTH_FIELD_KEY} which contains a JSON object with a <code>keycloak</code> field which can be
//	 * mapped to a {@link EndpointOptions} instance, the current path is added to the {@link #temporaryEndpoints} with the loaded configuration. Afterwards the
//	 * usual {@link #handle handler} is called.
//	 * </p>
//	 *
//	 * @param rc
//	 *            The current routing context
//	 */
//	public void handleNodeAuth(RoutingContext rc) {
//
//		if (disabled) {
//			rc.next();
//			return;
//		}
//
//		String path = rc.pathParam("param0");
//		JsonObject node = rc.get(RC_NODE_TO_RENDER);
//		String pageAuthConfig = node == null
//			? null
//			: node.getJsonObject("fields", new JsonObject()).getString(RC_AUTH_FIELD_KEY, null);
//
//		log.debug("Checking if {} needs authentication", path);
//
//		if (pageAuthConfig == null || pageAuthConfig.isEmpty()) {
//			log.trace("No authentication required");
//			rc.next();
//
//			return;
//		}
//
//		JsonObject pageAuth;
//
//		if (Boolean.TRUE.equals(rc.get(RC_AUTH_DONE_KEY))) {
//			log.debug("Authentication already done for {}", path);
//			rc.next();
//			return;
//		}
//
//		if (temporaryEndpoints.containsKey(path)) {
//			// The page could have been republished without need for authentication in the meantime.
//			temporaryEndpoints.remove(path);
//		}
//
//		try {
//			pageAuth = new JsonObject(pageAuthConfig);
//		} catch (DecodeException e) {
//			log.warn("Invalid authentication config: {}", e, pageAuthConfig);
//			rc.next();
//			return;
//		}
//
//		JsonObject keycloak = pageAuth.getJsonObject("keycloak");
//
//		if (keycloak == null) {
//			log.warn("No supported authentication method specified");
//			rc.next();
//			return;
//		}
//
//		EndpointOptions endpoint = keycloak.mapTo(EndpointOptions.class);
//
//		if (log.isInfoEnabled()) {
//			String perm;
//			List<String> perms = endpoint.getPermissions();
//
//			if (perms.isEmpty()) {
//				perm = "without special permissions";
//			} else {
//				perm = "with permissions {" + endpoint.getPermissionMode().name() + " " + perms + "}";
//			}
//
//			log.info("Adding {} as temporary enpoint {}", path, perm);
//		}
//
//		temporaryEndpoints.put(path, endpoint);
//
//		// When the authentication requires redirects, it may be necessary to restart the routing
//		// process for the current request once authentication is finished.
//		rc.put(RC_AUTH_REROUTE_KEY, Boolean.TRUE);
//		handle(rc);
//	}
//
//	/**
//	 * Checks if any authentication of authorisation actions need to be performed.
//	 *
//	 * <p>
//	 * When there is no user in the active routing context, but the {@link #COOKIE_AUTH} cookie is set, the access token is loaded from the cookie. If neither
//	 * are set, we redirect to the {@link #checkEndpoint} to start the OAuth2 process.
//	 * </p>
//	 *
//	 * <p>
//	 * Afterwards we check if the access token is expired and we need to refresh it. And finally {@link #internalHandle} is called to check for necessary
//	 * authorisations.
//	 * </p>
//	 */
//	@Override
//	public void handle(RoutingContext rc) {
//
//		if (disabled) {
//			log.warn("Authentication handler was called despite beeing disabled");
//			rc.next();
//			return;
//		}
//
//		if (rc.user() == null) {
//			if (rc.getCookie(COOKIE_AUTH) == null) {
//				log.debug("No user information found, initiating authorisation");
//
//				prepareRedirect(rc, rc.request().absoluteURI());
//
//				rc.response()
//					.setStatusCode(SEE_OTHER.code())
//					.putHeader(HttpHeaders.LOCATION, checkEndpoint)
//					.end();
//				return;
//			}
//
//			setUser(rc);
//		}
//
//		AccessToken token = (AccessToken) rc.user();
//
//		if (token.expired()) {
//			log.info("Trying to refresh expired token");
//			token.refresh(result -> refreshCallback(rc, result));
//			return;
//		}
//
//		log.debug("User authenticated");
//		internalHandle(rc);
//	}
//
//	/**
//	 * Performs some sanity checks on configured {@link AuthenticationOptions#getEndPoints() protected endpoints} and adds them to {@link #staticEndpoints}.
//	 *
//	 * @param endpoint
//	 *            The endpoint to add
//	 *
//	 * @return <code>true</code> when a valid path was configured in the endpoint, and <code>false</code> otherwise
//	 */
//	private boolean addEndpoint(EndpointOptions endpoint) {
//		String path = endpoint.getPath();
//		int idx = path.indexOf('*');
//
//		if (idx >= 0) {
//			if (idx != path.length() - 1) {
//				// A wildcard is only allowed once at the end of a path.
//				log.warn("Ignoring invalid path \"{}\"", path);
//
//				return false;
//			}
//
//			path = path.substring(0, idx);
//		}
//
//		List<String> perms = endpoint.getPermissions();
//
//		if (!perms.isEmpty()) {
//			staticEndpoints.put(path, endpoint);
//		}
//
//		if (log.isDebugEnabled()) {
//			String perm;
//
//			if (perms.isEmpty()) {
//				perm = "without special permissions";
//			} else {
//				perm = "with permissions {" + endpoint.getPermissionMode().name() + " " + perms + "}";
//			}
//
//			log.debug("Adding {} as protected enpoint {}", endpoint.getPath(), perm);
//		}
//
//		return true;
//	}
//
//	/**
//	 * Sets the {@link #COOKIE_AUTH_REDIRECT redirect cookie} with the specified value.
//	 *
//	 * @param rc
//	 *            The current routing context
//	 * @param redirect
//	 *            The redirect path
//	 */
//	private void prepareRedirect(RoutingContext rc, String redirect) {
//		Cookie cookie = rc.getCookie(COOKIE_AUTH_REDIRECT);
//
//		if (cookie == null) {
//			if (redirect == null || redirect.isEmpty()) {
//				log.debug("Redirect not specified, using default");
//				redirect = DEFAULT_REDIRECT;
//			} else {
//				log.debug("Preparing authentication with redirect: {}", redirect);
//			}
//
//			cookie = Cookie.cookie(COOKIE_AUTH_REDIRECT, redirect).setPath("/").setMaxAge(60);
//			rc.addCookie(cookie);
//		} else if (log.isDebugEnabled()) {
//			log.debug("Authentication already in progress with redirect: {}", cookie.getValue());
//		}
//	}
//
//	/**
//	 * Stores the current access token in the authorisation cookie and redirects to the location in the {@link #COOKIE_AUTH_REDIRECT} if present.
//	 *
//	 * @param rc
//	 *            The current routing context
//	 */
//	private void handlePostAuth(RoutingContext rc) {
//		setAuthCookie(rc);
//
//		Cookie redirectCookie = rc.getCookie(COOKIE_AUTH_REDIRECT);
//
//		if (redirectCookie == null) {
//			log.warn("No redirect cookie found, rerouting to \"/\"");
//			rc.reroute(DEFAULT_REDIRECT);
//		} else {
//			String redirect = redirectCookie.getValue();
//
//			log.debug("Redirecting to {}", redirect);
//
//			rc.addCookie(Cookie.cookie(COOKIE_AUTH_REDIRECT, "deleted")
//				.setPath("/")
//				.setMaxAge(0));
//
//			rc.response()
//				.setStatusCode(SEE_OTHER.code())
//				.putHeader(HttpHeaders.LOCATION, redirect)
//				.end();
//		}
//	}
//
//	/**
//	 * Loads the access token from the {@link #COOKIE_AUTH authorisation cookie} and stores it in the routing context.
//	 *
//	 * @param rc
//	 *            The current routing context
//	 */
//	private void setUser(RoutingContext rc) {
//		if (rc.user() != null) {
//			if (rc.get(RC_AUTH_TOKEN_KEY) == null) {
//				rc.put(RC_AUTH_TOKEN_KEY, rc.user().principal().getString("access_token"));
//			}
//
//			return;
//		}
//
//		Cookie cookie = rc.getCookie(COOKIE_AUTH);
//
//		if (cookie == null) {
//			log.trace("No authentication cookie set");
//
//			return;
//		}
//
//		JsonObject token = new JsonObject();
//
//		// FIXME: This is quite ugly and definitely not in the spirit of OAuth2,
//		// but if the portal should avoid "unnecessary" requests to Keycloak
//		// something like this is unavoidable.
//		token.put("access_token", rc.getCookie(COOKIE_AUTH).getValue());
//		token.put("refresh_token", rc.getCookie(COOKIE_AUTH_REFRESH).getValue());
//
//		User user = new AccessTokenImpl((OAuth2AuthProviderImpl) provider, token);
//
//		rc.setUser(user);
//		rc.put(RC_AUTH_TOKEN_KEY, user.principal().getString("access_token"));
//	}
//
//	/**
//	 * Finishes the authentication handling by restarting the routing to the current path or just calling the next handler.
//	 *
//	 * Rerouting is done, when the authentication was triggered by the loaded Mesh node itself and not by a path specified in the portal configuration.
//	 *
//	 * @param rc
//	 *            The current routing context
//	 * @param path
//	 *            The path to reroute to
//	 */
//	private void internalHandleDone(RoutingContext rc, String path) {
//		if (Boolean.TRUE.equals(rc.get(RC_AUTH_REROUTE_KEY))) {
//			log.trace("Rerouting with path {}", path);
//			rc.put(RC_AUTH_DONE_KEY, Boolean.TRUE);
//			rc.reroute(path);
//		} else {
//			log.trace("Proceeding to next handler");
//			rc.next();
//		}
//	}
//
//	/**
//	 * Checks if the currently logged in user has the necessary roles to access the requested resource.
//	 *
//	 * @param rc
//	 *            The current routing context
//	 */
//	private void internalHandle(RoutingContext rc) {
//		List<String> perms;
//		String routePath = rc.currentRoute().getPath();
//		String path = routePath == null || routePath.isEmpty() ? rc.pathParam("param0") : routePath;
//		EndpointOptions endpoint = temporaryEndpoints.get(path);
//
//		if (endpoint == null) {
//			endpoint = staticEndpoints.get(path);
//		}
//
//		if (endpoint == null || (perms = endpoint.getPermissions()).isEmpty()) {
//			log.debug("No special permissions required for {}", path);
//			internalHandleDone(rc, path);
//
//			return;
//		}
//
//		if (log.isDebugEnabled()) {
//			log.debug(
//				"Checking authorisation for {}: {{} {}}",
//				path,
//				endpoint.getPermissionMode().name(),
//				perms);
//		}
//
//		AuthorisationCheckHandler permChecker = AuthorisationCheckHandler.create(endpoint.getPermissionMode());
//
//		for (String perm : perms) {
//			log.trace("Checking for permission \"{}\"", perm);
//			rc.user().isAuthorized(perm, permChecker);
//		}
//
//		if (permChecker.isAuthorised()) {
//			log.debug("User is authorised");
//			internalHandleDone(rc, path);
//		} else {
//			String redirect = portalConfig.getAuthOptions().getNotAuthorisedPage();
//
//			log.debug("User is not authorised, redirecting to: {}", redirect);
//
//			if (redirect != null && !redirect.isEmpty()) {
//				rc.reroute(redirect);
//			} else {
//				log.warn("No redirect set for unauthorised users");
//				rc.response().setStatusCode(UNAUTHORIZED.code()).end();
//			}
//		}
//	}
//
//	/**
//	 * Sets the authentication and the refresh cookie.
//	 *
//	 * @param rc
//	 *            The current routing context
//	 */
//	private void setAuthCookie(RoutingContext rc) {
//		JsonObject principal = rc.user().principal();
//
//		if (log.isTraceEnabled()) {
//			log.trace("Authenticated:\n{}", principal.encodePrettily());
//
//			if (extraConfig.containsKey("realm-public-key")) {
//				JWT jwt = new JWT().addPublicKey("RS256", extraConfig.getString("realm-public-key"));
//
//				log.trace("Decoded access token:\n{}", jwt.decode(principal.getString("access_token")).encodePrettily());
//			}
//		}
//
//		Cookie cookie = Cookie.cookie(COOKIE_AUTH, principal.getString("access_token"))
//			.setPath("/")
//			.setMaxAge(principal.getLong("expires_in", DEFAULT_AUTH_TIMEOUT));
//
//		rc.addCookie(cookie);
//
//		cookie = Cookie.cookie(COOKIE_AUTH_REFRESH, principal.getString("refresh_token"))
//			.setPath("/")
//			.setMaxAge(principal.getLong("refresh_expires_in", DEFAULT_AUTH_TIMEOUT));
//
//		rc.addCookie(cookie);
//	}
//
//	/**
//	 * Callback for the token refresh procedure.
//	 *
//	 * Just stores the new access token in the routing context.
//	 *
//	 * @param rc
//	 *            The current routing context
//	 * @param result
//	 *            The result of the token refresh
//	 */
//	private void refreshCallback(RoutingContext rc, AsyncResult<Void> result) {
//		if (result.failed()) {
//			log.fatal("Refreshing access token failed", result.cause());
//
//			rc.response().setStatusCode(INTERNAL_SERVER_ERROR.code()).end();
//
//			return;
//		}
//
//		log.info("Token refresh successful");
//		setAuthCookie(rc);
//		internalHandle(rc);
//	}
//
//}
