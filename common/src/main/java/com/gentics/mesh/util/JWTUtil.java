package com.gentics.mesh.util;

import com.gentics.mesh.etc.config.AuthenticationOptions;

import io.vertx.ext.jwt.JWTOptions;

public class JWTUtil {
	
	public static JWTOptions createJWTOptions(AuthenticationOptions options) {
		return new JWTOptions()
			.setLeeway(options.getLeeway())
			.setIssuer(options.getIssuer())
			.setAudience(options.getAudience())
			.setIgnoreExpiration(options.isIgnoreExpiration());
	}
}
