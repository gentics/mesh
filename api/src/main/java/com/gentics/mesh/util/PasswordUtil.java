package com.gentics.mesh.util;

import java.util.Random;

/**
 * Utility class which provides methods to handle passwords.
 */
public final class PasswordUtil {

	private static final String ALPHA_NUMERIC_READABLE_CHAR = "ABCDEFGHJKLMNPQRTUVWXYZ"
		+ "2346789"
		+ "abcdefghjkmnpqrtuvxyz";

	private static final int DEFAULT_PW_LEN = 6;

	private PasswordUtil() {
	}

	/**
	 * Generate human readable password with given length.
	 * 
	 * @param len
	 * @return
	 */
	public static String humanPassword(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			int index = new Random().nextInt(ALPHA_NUMERIC_READABLE_CHAR.length());
			sb.append(ALPHA_NUMERIC_READABLE_CHAR.charAt(index));
		}
		return sb.toString();
	}

	/**
	 * Generate human readable password.
	 * 
	 * @return
	 */
	public static String humanPassword() {
		return humanPassword(DEFAULT_PW_LEN);
	}
}
