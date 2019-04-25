package com.gentics.mesh.util;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class DateUtils {

	private static final Logger log = LoggerFactory.getLogger(DateUtils.class);

	static ZoneOffset zoneOffset;

	static {
		OffsetDateTime odt = OffsetDateTime.now(ZoneId.systemDefault());
		zoneOffset = odt.getOffset();
	}

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
		return toZonedDateTime(timeInMs).format(DateTimeFormatter.ISO_INSTANT);
	}

	/**
	 * Convert the provided unixtimestamp (miliseconds since midnight, January 1, 1970 UTC) to zoned date time
	 * 
	 * @param timeInMs
	 * @return
	 */
	public static ZonedDateTime toZonedDateTime(long timeInMs) {
		return Instant.ofEpochSecond(timeInMs / 1000).atZone(ZoneOffset.UTC);
	}

	public static Long fromISO8601(String dateString) {
		return fromISO8601(dateString, false);
	}

	/**
	 * Converts the provided date string into an unixtimestamp value.
	 * 
	 * @param dateString
	 * @param failOnFormatError
	 * @return Unixtimestamp value or null if the provided date string was also null.
	 */
	public static Long fromISO8601(String dateString, boolean failOnFormatError) {
		if (dateString == null) {
			return null;
		}
		try {
			OffsetDateTime odt = OffsetDateTime.parse(dateString);
			return odt.toInstant().toEpochMilli();
		} catch (DateTimeParseException e) {
			if (log.isDebugEnabled()) {
				log.debug("Error while parsing date {" + dateString + "}. Using fallback.", e);
			}
			try {
				// We also support date strings which do not include an offset. We apply the system offset in those cases.
				Long date = LocalDateTime.parse(dateString).toInstant(zoneOffset).toEpochMilli();
				return date;
			} catch (DateTimeParseException e2) {
				if (log.isDebugEnabled()) {
					log.debug("Fallback failed with exception", e);
				}
				if (failOnFormatError) {
					throw error(BAD_REQUEST, "error_date_format_invalid", dateString);
				}
			}
		}
		return null;

	}

	/**
	 * Check whether the date can be parsed.
	 * 
	 * @param dateString
	 * @return
	 */
	public static boolean isDate(String dateString) {
		Long date = fromISO8601(dateString);
		return date != null;
	}

}
