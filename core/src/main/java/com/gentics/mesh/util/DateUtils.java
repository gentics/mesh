package com.gentics.mesh.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

	/**
	 * Convert the provided unixtimestamp (inMs) to a ISO8601 string.
	 * 
	 * @param timestampInMs
	 * @param fallback
	 * @return
	 */
	public static String toISO8601(Long timestampInMs, long fallback) {
		long time = timestampInMs == null ? fallback : timestampInMs;
		return Instant.ofEpochSecond(time / 1000).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT);
	}

}
