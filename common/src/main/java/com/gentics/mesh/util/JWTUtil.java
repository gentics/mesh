package com.gentics.mesh.util;

import com.gentics.mesh.etc.config.AuthenticationOptions;

import io.vertx.ext.jwt.JWTOptions;

/**
 * Utility class which contains static helper functions for JWT creation and manipulation.
 */
public class JWTUtil {

	private JWTUtil() {}

	public static final String JWT_FIELD_EXPIRATION = "exp";
	public static final String JWT_FIELD_ISSUER = "iss";
	public static final String JWT_FIELD_AUDIENCE = "aud";

	public static JWTOptions createJWTOptions(AuthenticationOptions options) {
		return new JWTOptions()
			.setAlgorithm(options.getAlgorithm())
			.setLeeway(options.getLeeway())
			.setIssuer(options.getIssuer())
			.setAudience(options.getAudience())
			.setIgnoreExpiration(options.isIgnoreExpiration())
			.setExpiresInSeconds(options.getTokenExpirationTime());
	}
}
