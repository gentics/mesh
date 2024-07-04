package com.gentics.mesh.plugin.common.util;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;

/**
 * Utility methods for handling JWTs.
 */
public class AuthUtil {

	private static final Logger log = LoggerFactory.getLogger(AuthUtil.class);

	/**
	 * Tries to decode the given string as a JWT and returns the payload part.
	 *
	 * <strong>caution:</strong> this method will <em>not</em> check the tokens signature, and should not be used for verification.
	 *
	 * @param token the jwt to be decoded
	 *
	 * @return the payload part of the given jwt as a <code>jsonobject</code>
	 */
	public static JsonObject decodeToken(String token) {
		return decodeToken(token, true);
	}

	/**
	 * Tries to decode the given string as a JWT and returns the payload part.
	 *
	 * <strong>Caution:</strong> this method will <em>not</em> check the tokens signature, and should not be used for verification.
	 *
	 * @param token The JWT to be decoded
	 *
	 * @return The payload part of the given JWT as a <code>JsonObject</code>
	 */
	public static JsonObject decodeToken(String token, boolean payloadOnly) {
		if (token != null) {
			try {
				String[] parts = token.split("\\.", 3);

				if (parts.length != 3) {
					log.error("Given token is not a JWT: {}", token);
					return new JsonObject();
				}

				if (payloadOnly) {
					return decodeTokenPart(parts[1]);
				}

				return new JsonObject()
					.put("head", decodeTokenPart(parts[0]))
					.put("payload", decodeTokenPart(parts[1]))
					.put("signature", parts[2]);
			} catch (Exception e) {
				log.error("Could not decode token: " + e.getLocalizedMessage() + "\n" + token, e);
			}
		} else {
			log.warn("No token specified");
		}

		return new JsonObject();
	}

	/**
	 * Creates a {@link JsonObject} from the given Base64 web encoded String.
	 *
	 * @param part The JWT part to decode
	 * @return The <code>JsonObject</code> for the given JWT part
	 */
	private static JsonObject decodeTokenPart(String part) {
		try {
			return new JsonObject(new String(Base64.decodeBase64(part)));
		} catch (Exception e) {
			log.error("Could not decode token part: {}\nPart: {}", e.getMessage(), part);
		}

		return new JsonObject();
	}
}
