package com.gentics.mesh.plugin.common.helper;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Handlebars helper to format date with a given pattern.
 *
 * <p>
 *     Example: {{date reference.edited "MM.dd.YYYY"}}
 * </p>
 *
 * <p>
 *     The original date must be a valid {@link DateTimeFormatter#ISO_INSTANT}
 *     in UTC.
 * </p>
 */
public class DateHelper implements Helper<String> {

	private static final Logger log = LoggerFactory.getLogger(DateHelper.class);

	@Override
	public String apply(String date, Options options) throws IOException {
		// We need to get the dateFormat from the options to satisfy the Helper interface.
		String dateFormat = options.param(0, null);

		if (date == null || dateFormat == null) {
			log.warn(String.format("Missing parameters. Expected ISO Date and date format string. Got '%s' and '%s'", date, dateFormat));
			return "";
		}

		Instant parsedDate;
		try {
			parsedDate = Instant.parse(date);
		} catch (Throwable e) {
			log.warn("Could not parse date", e);
			return "";
		}

		DateTimeFormatter formatter;
		try {
			formatter = DateTimeFormatter.ofPattern(dateFormat);
		} catch (Throwable e) {
			log.warn("Could not parse date format string", e);
			return "";
		}

		return parsedDate.atZone(ZoneId.systemDefault()).format(formatter);
	}
}
