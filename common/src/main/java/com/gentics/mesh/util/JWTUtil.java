package com.gentics.mesh.util;

import java.util.List;

import com.gentics.mesh.etc.config.AuthenticationOptions;

import io.vertx.ext.jwt.JWTOptions;

/**
 * Utility class which contains static helper functions for JWT creation and manipulation.
 */
public class JWTUtil {

	/**
	 * Helper class which delegates the currently set Authentication-Options as JWTOption.
	 * This is currently only useful for testing, as the options change after each test and need to be
	 * represented in the JWT signing/validation process.
	 */
	private static class JWTDelegateOptions extends JWTOptions {
		private AuthenticationOptions options;

		public JWTDelegateOptions(AuthenticationOptions options) {
			this.options = options;
		}

		@Override
		public int getExpiresInSeconds() {
			return options.getTokenExpirationTime();
		}

		@Override
		public JWTOptions setExpiresInSeconds(int tokenExpirationTime) {
			options.setTokenExpirationTime(tokenExpirationTime / 60);
			return this;
		}

		@Override
		public JWTOptions setExpiresInMinutes(int expiresInMinutes) {
			options.setTokenExpirationTime(expiresInMinutes);
			return this;
		}

		@Override
		public String getAlgorithm() {
			return options.getAlgorithm();
		}

		@Override
		public JWTOptions setAlgorithm(String algorithm) {
			options.setAlgorithm(algorithm);
			return this;
		}

		@Override
		public int getLeeway() {
			return options.getLeeway();
		}

		@Override
		public JWTOptions setLeeway(int leeway) {
			options.setLeeway(leeway);
			return this;
		}

		@Override
		public String getIssuer() {
			return options.getIssuer();
		}

		@Override
		public JWTOptions setIssuer(String issuer) {
			options.setIssuer(issuer);
			return this;
		}

		@Override
		public List<String> getAudience() {
			return options.getAudience();
		}

		@Override
		public JWTOptions setAudience(List<String> audience) {
			options.setAudience(audience);
			return this;
		}

		@Override
		public boolean isIgnoreExpiration() {
			return options.isIgnoreExpiration();
		}

		@Override
		public JWTOptions setIgnoreExpiration(boolean ignoreExpiration) {
			options.setIgnoreExpiration(ignoreExpiration);
			return this;
		}
	}

	private JWTUtil() {}

	public static final String JWT_FIELD_EXPIRATION = "exp";
	public static final String JWT_FIELD_ISSUER = "iss";
	public static final String JWT_FIELD_AUDIENCE = "aud";

	public static JWTOptions createJWTOptions(AuthenticationOptions options) {
		return new JWTDelegateOptions(options);
	}
}
