package com.gentics.mesh.plugin.comment.util;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Example: {{date reference.edited "MM.dd.YYYY"}}
 */
public class DateHelper implements Helper<String> {
	private static final Logger log = LoggerFactory.getLogger(DateHelper.class);

	@Override
	public String apply(String date, Options options) throws IOException {
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
