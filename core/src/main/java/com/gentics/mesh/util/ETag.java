package com.gentics.mesh.util;

import java.nio.charset.Charset;

import com.google.common.hash.Hashing;

public class ETag {

	/**
	 * Hash the given key in order to generate a uniform etag hash.
	 * 
	 * @param key
	 *            Key which should be hashed
	 * @return Computed hash
	 */
	public static String hash(String key) {
		return Hashing.crc32c().hashString(key.toString(), Charset.defaultCharset()).toString();
	}

}
