package com.gentics.mesh.auth;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.JWTUtil;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.handler.JWTAuthHandler;

/**
 * Stores the auth handler which fits the current set of keys
 */
@Singleton
public class AuthHandlerContainer {

	private static final Logger log = LoggerFactory.getLogger(AuthHandlerContainer.class);

	private JWTAuthHandler authHandler;
	private int hashCode;

	@Inject
	public Vertx vertx;

	@Inject
	public MeshOptions options;

	@Inject
	public AuthHandlerContainer() {}

	/**
	 * Create a new JWT handler for the given JWKs.
	 *
	 * @param jwks
	 * @return
	 */
	public synchronized JWTAuthHandler create(Set<JsonObject> jwks) {
		if (hashCode != jwks.hashCode()) {
			JWTAuthOptions jwtOptions = new JWTAuthOptions();
			// Set JWT options from the config
			jwtOptions.setJWTOptions(JWTUtil.createJWTOptions(options.getAuthenticationOptions()));
			// Now add all keys to jwt config
			for (JsonObject key : jwks) {
				jwtOptions.addJwk(key);
			}
			log.debug("Keys changed. Creating a new auth handler to be used.");
			JWTAuth authProvider = JWTAuth.create(vertx, jwtOptions);
			authHandler = JWTAuthHandler.create(authProvider);
			hashCode = jwks.hashCode();
		}
		return authHandler;
	}
}
