package com.gentics.mesh.util;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Main source for UUIDs. The UUIDs are shorted in order to better utilize the database indices.
 */
public final class UUIDUtil {

	private static Pattern p = Pattern.compile("^[A-Fa-f0-9]+$");
	private static Pattern shortUuid = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
	private static final String WITH_DASHES = "$1-$2-$3-$4-$5";

	private UUIDUtil() {

	}

	/**
	 * You can run this class to generate a UUID. Useful when needing random uuids for documentation.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(UUIDUtil.randomUUID());
	}

	/**
	 * Convert a shortened uuid into a uuid which includes dashes
	 * 
	 * @param uuid
	 * @return UUID which contains dashes
	 */
	public static String toFullUuid(String uuid) {
		return shortUuid.matcher(uuid).replaceAll(WITH_DASHES);
	}

	/**
	 * Convert a uuid with dashes to a uuid without dashes.
	 * 
	 * @param uuid
	 * @return UUID without dashes
	 */
	public static String toShortUuid(String uuid) {
		return uuid.replaceAll("-", "");
	}

	/**
	 * Create a random UUID string which does not include dashes.
	 * 
	 * @return Random generated uuid without dashes
	 */
	public static String randomUUID() {
		final UUID uuid = UUID.randomUUID();
		return (digits(uuid.getMostSignificantBits() >> 32, 8) + digits(uuid.getMostSignificantBits() >> 16, 4)
			+ digits(uuid.getMostSignificantBits(), 4) + digits(uuid.getLeastSignificantBits() >> 48, 4)
			+ digits(uuid.getLeastSignificantBits(), 12));
	}

	/**
	 * Returns val represented by the specified number of hex digits.
	 * 
	 * @param val
	 * @param digits
	 * @return Hex String
	 */
	private static String digits(long val, int digits) {
		long hi = 1L << (digits * 4);
		return Long.toHexString(hi | (val & (hi - 1))).substring(1);
	}

	/**
	 * Check whether the given text is a uuid.
	 * 
	 * @param text
	 *            String to be checked
	 * @return true if the text represents a uuid
	 */
	public static boolean isUUID(String text) {
		if (text == null || text.length() != 32) {
			return false;
		} else {
			return p.matcher(text).matches();
		}
	}

	/**
	 * Convert a mesh style uuid to a java {@link UUID}.
	 * 
	 * @param uuid
	 * @return
	 */
	public static UUID toJavaUuid(String uuid) {
		if (uuid == null) {
			return null;
		}
		return UUID.fromString(toFullUuid(uuid));
	}

	/**
	 * Convert a uuid with dashes to a uuid without dashes.
	 *
	 * @return
	 * @param uuid
	 */
	public static String toShortUuid(UUID uuid) {
		if (uuid == null) {
			return null;
		}
		return toShortUuid(uuid.toString());
	}
}
