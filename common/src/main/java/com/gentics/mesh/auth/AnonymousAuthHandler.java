package com.gentics.mesh.auth;

import javax.inject.Inject;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.http.MeshHeaders;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class AnonymousAuthHandler implements Handler<RoutingContext> {

	public static final String ANONYMOUS_USERNAME = "anonymous";

	private static final Logger log = LoggerFactory.getLogger(AnonymousAuthHandler.class);
	private Database db;
	private BootstrapInitializer boot;

	@Inject
	public AnonymousAuthHandler(Database db, BootstrapInitializer boot) {
		this.db = db;
		this.boot = boot;
	}

	@Override
	public void handle(RoutingContext rc) {
		if (Mesh.mesh().getOptions().getAuthenticationOptions().isEnableAnonymousAccess()) {
			final HttpServerRequest request = rc.request();
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
			} else {
				rc.setUser(anonymousUser);
				authorizeUser(anonymousUser, rc);
				return;
			}
		}
	}

	private void authorizeUser(MeshAuthUser anonymousUser, RoutingContext rc) {
		// TODO implement me
	}

	private void handle401(RoutingContext context) {
		context.fail(401);
	}

}
