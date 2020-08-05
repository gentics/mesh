package com.gentics.mesh.util;

import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hashing;

public class ETag {

	/**
	 * Hash the given key in order to generate a uniform etag hash.
	 * 
	 * @param builder
	 *            builder which provides the keys of the string that will be hashed
	 * @return Computed hash
	 */
	public static String hash(StringBuilder builder) {
		return hash(builder.toString());
	}

	/**
	 * Hash the given key in order to generate a uniform etag hash.
	 * 
	 * @param key
	 *            Key which should be hashed
	 * @return Computed hash
	 */
	public static String hash(String key) {
		return Hashing.crc32c().hashString(key.toString(), StandardCharsets.UTF_8).toString();
	}

	/**
	 * Hash the given int in order to generate a uniform etag hash.
	 *
	 * @param key
	 *            Key which should be hashed
	 * @return Computed hash
	 */
	public static String hash(int key) {
		return Hashing.crc32c().hashInt(key).toString();
	}

	/**
	 * Wrap the given etag with the needed quotes and add the weak flag if needed.
	 * 
	 * @param entityTag
	 *            Tag to be used for construction.
	 * @param isWeak
	 *            Flag which indicates whether the provides tag is an weak tag.
	 * @return
	 */
	public static String prepareHeader(String entityTag, boolean isWeak) {
		StringBuilder builder = new StringBuilder();
		if (isWeak) {
			builder.append("W/");
		}
		builder.append('"');
		builder.append(entityTag);
		builder.append('"');
		return builder.toString();
	}

	/**
	 * Extracts the etag from the provided header value.
	 * 
	 * @param headerValue
	 * @return
	 */
	public static String extract(String headerValue) {
		if (headerValue == null) {
			return null;
		}
		return headerValue.substring(headerValue.indexOf("\"") + 1, headerValue.lastIndexOf("\""));
	}

}
