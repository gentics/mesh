package com.gentics.mesh.auth.handler;

import static io.vertx.core.http.HttpHeaders.AUTHORIZATION;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.http.MeshHeaders;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;

/**
 * Auth handler which will deal with anonymous auth handling. This handler will only auth the user if anonymous auth is enabled and the request does not contain
 * any auth header.
 */
@Singleton
public class MeshAnonymousAuthHandler extends AuthHandlerImpl implements MeshAuthHandler {

	public static final String ANONYMOUS_USERNAME = "anonymous";

	private static final Logger log = LoggerFactory.getLogger(MeshAnonymousAuthHandler.class);
	private LegacyDatabase db;
	private BootstrapInitializer boot;

	private MeshOptions options;

	@Inject
	public MeshAnonymousAuthHandler(MeshJWTAuthProvider authProvider, MeshOptions options, LegacyDatabase db, BootstrapInitializer boot) {
		super(authProvider);
		this.options = options;
		this.db = db;
		this.boot = boot;
	}

	@Override
	public void parseCredentials(RoutingContext arg0, Handler<AsyncResult<JsonObject>> arg1) {
		// Not needed for this handler
	}

	@Override
	public void handle(RoutingContext rc) {
		// Do nothing if the user has already been authenticated
		if (rc.user() != null) {
			rc.next();
			return;
		}

		final HttpServerRequest request = rc.request();
		final String authorization = request.headers().get(AUTHORIZATION);
		boolean hasAuth = authorization != null;
		boolean isAnonEnabled = options.getAuthenticationOptions().isEnableAnonymousAccess();

		// The anonymous handler is the last handler in the chain.
		// The auth information could not be used by any previous handler thus it is safe to 401 here.
		if (hasAuth) {
			handle401(rc);
			return;
		}

		if (isAnonEnabled) {
			if (log.isDebugEnabled()) {
				log.debug("No Authorization header was found.");
			}
			// Check whether the Anonymous-Authentication header was set to disable. This will disable the anonymous authentication method altogether.
			String anonymousAuthHeaderValue = request.headers().get(MeshHeaders.ANONYMOUS_AUTHENTICATION);
			if ("disable".equals(anonymousAuthHeaderValue)) {
				handle401(rc);
				return;
			}
			if (log.isDebugEnabled()) {
				log.debug("Using anonymous user.");
			}
			MeshAuthUser anonymousUser = db.tx(() -> boot.userRoot().findMeshAuthUserByUsername(ANONYMOUS_USERNAME));
			if (anonymousUser == null) {
				if (log.isDebugEnabled()) {
					log.debug("No anonymous user and authorization header was found. Can't authenticate request.");
				}
				handle401(rc);
				return;
			} else {
				rc.setUser(anonymousUser);
			}
			rc.next();
		} else {
			handle401(rc);
			return;
		}

	}

}
