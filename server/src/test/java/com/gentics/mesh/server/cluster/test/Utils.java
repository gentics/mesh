package com.gentics.mesh.server.cluster.test;

import java.util.UUID;

/**
 * Various test utility methods.
 */
public final class Utils {

	private Utils() {
	}

	/**
	 * Generate a new random UUID.
	 * 
	 * @return
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
	 * @return
	 */
	private static String digits(long val, int digits) {
		long hi = 1L << (digits * 4);
		return Long.toHexString(hi | (val & (hi - 1))).substring(1);
	}

	/**
	 * Sleep given amount of time.
	 * 
	 * @param time Time in milliseconds
	 */
	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
