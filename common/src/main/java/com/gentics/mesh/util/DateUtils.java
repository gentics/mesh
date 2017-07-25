package com.gentics.mesh.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.joda.time.DateTime;

public final class DateUtils {

	/**
	 * Convert the provided unixtimestamp (miliseconds since midnight, January 1, 1970 UTC) to an ISO8601 string. Use the fallback timestamp if the provided
	 * timestamp is null.
	 * 
	 * @param timestampInMs
	 * @param fallback
	 * @return
	 */
	public static String toISO8601(Long timestampInMs, long fallback) {
		long time = timestampInMs == null ? fallback : timestampInMs;
		return toISO8601(time);
	}

	/**
	 * Convert the provided unixtimestamp (miliseconds since midnight, January 1, 1970 UTC) to an ISO8601 string.
	 * 
	 * @param timeInMs
	 * @return
	 */
	public static String toISO8601(long timeInMs) {
		return Instant.ofEpochSecond(timeInMs / 1000).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT);
	}

	/**
	 * Converts the provided date string into an unixtimestamp value.
	 * 
	 * @param dateString
	 * @return Unixtimestamp value or null if the provided date string was also null.
	 */
	public static Long fromISO8601(String dateString) {
		if (dateString == null) {
			return null;
		}
		return new DateTime(dateString).toDate().getTime();
	}

}
