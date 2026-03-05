package com.gentics.mesh.plugin.forms.helper;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.model.ValueListElement;
import com.github.jknack.handlebars.Options;

import io.vertx.core.json.JsonObject;

/**
 * Helper for rendering a form (optionally prefilled with input and error messages)
 */
public class FormHelper {

	public final static String SUBTYPE_PARAMETER_NAME = "subtype";
	public final static String DEFAULT_SUBTYPE_PARAMETER_NAME = "defaultSubtype";
	public final static String SRC_FORMAT = "srcFormat";

	private final static Logger log = LoggerFactory.getLogger(FormHelper.class);

	/**
	 * Create an instance
	 */
	public FormHelper() {
	}

	public CharSequence equals(Object o1, Object o2, Options options) throws IOException {
		if (Objects.equals(o1, o2)) {
			return options.fn();
		}
		return options.inverse();
	}

	public CharSequence startsWith(String str, String prefix, Options options) throws IOException {
    		return str != null && prefix != null && str.startsWith(prefix) ? options.fn() : options.inverse();
	}

	/**
	 * Render the name of the template for the element with type given in the context
	 *
	 * @param elementType element type
	 * @param options options
	 * @return template name
	 */
	public CharSequence formsElementTemplate(Object elementType, Options options) {
		return elementTemplate(elementType, "elements", options);
	}

	/**
	 * Render the name of the poll results template for the element with type given in the context
	 *
	 * @param elementType element type
	 * @param options options
	 * @return template name
	 */
	public CharSequence pollResultTemplate(Object elementType, Options options) {
		if (elementType instanceof String) {
			switch ((String) elementType) {
			case "checkbox":
			case "selectgroup":
				return elementTemplate(elementType, "poll-results", options);

			default:
				return elementTemplate("generic", "poll-results", options);
			}
		}

		return "";
	}

	/**
	 * Render the name of the template for the element with type given in the context
	 *
	 * @param elementType element type
	 * @param baseDir directory name
	 * @param options options
	 * @return template name
	 */
	public CharSequence elementTemplate(Object elementType, String baseDir, Options options) {
		Object elementSubType = options.hash.get(SUBTYPE_PARAMETER_NAME);
		// to prevent and endless loop if the helper is called with "null" in the subtype parameter
		// require a default subtype parameter to fallback to
		// example:
		//   (formsElementTemplate selectgroup subtype=null defaultSubtype="singleselect")
		//   ... this will get the template selectgroup_singleselect
		if (elementSubType == null) {
			elementSubType = options.hash.get(DEFAULT_SUBTYPE_PARAMETER_NAME);
		}
		if (elementSubType != null) {
			return String.format("%s/%s_%s", baseDir, elementType, elementSubType);
		}
		return String.format("%s/%s", baseDir, elementType);
	}

	/**
	 * Determine the type of an input element
	 *
	 * @param context
	 *            context (which should be the input element)
	 * @param options
	 *            options
	 * @return type
	 */
	public CharSequence formsInputType(Object context, Options options) {
		if (context instanceof String) {
			switch ((String) context) {
			case "number":
			case "socialsecurity":
				return "number";
			case "telephone":
				return "tel";
			case "date":
			case "time":
			case "email":
				return (String) context;
			default:
				return "text";
			}
		} else if (context instanceof JsonObject) {
			JsonObject element = (JsonObject) context;
			int minVal = ParameterUtil.asInt(element.getValue("minVal"), -1);
			int maxVal = ParameterUtil.asInt(element.getValue("maxVal"), -1);

			if (minVal > 0 || maxVal > 0) {
				return "number";
			} else {
				return formsInputType(element.getString("validation", ""), options);
			}
		} else {
			return "text";
		}
	}

	/**
	 * Lookup the label for the given {@code key} from the {@code valueLabels}
	 * stored in the routing context.
	 *
	 * @param key The key to look up
	 * @param options The Handlebars options
	 * @return The label for {@code key} if it is present in {@code valueLabels},
	 * 		or {@code key} if not
	 */
	public Object getValueLabel(String key, Options options) {
		Object o = options.get(ValueListElement.RC_VALUE_LABELS);

		if (o instanceof JsonObject) {
			return ((JsonObject) o).getString(key, key);
		}

		return key;
	}

	/**
	 * Format given {@code date} with the specified {@code pattern}.
	 *
	 * <p>
	 *     The input date is expected to be an {@link DateTimeFormatter#ISO_DATE}
	 *     (YYYY-MM-DD), unless the optional parameter {@code srcFormat} is
	 *     specified).
	 * </p>
	 *
	 * <p>
	 *     If any of the parsing (date or source pattern) or formatting fails,
	 *     this helper will just return the input {@code date}.
	 * </p>
	 *
	 * @see #formatDateTime(Object, String, DateTimeFormatter, Options)
	 * @param date The date to be formatted
	 * @param pattern The target date format
	 * @param options Handlebars options
	 * @return The input {@code date} formatted with {@code pattern}
	 */
	public Object formatDate(Object date, String pattern, Options options) {
		return formatDateTime(date, pattern, DateTimeFormatter.ISO_DATE, options);
	}

	/**
	 * Format given {@code time} with the specified {@code pattern}.
	 *
	 * <p>
	 *     The input time is expected to be an {@link DateTimeFormatter#ISO_TIME}
	 *     (HH:mm[:ss]), unless the optional parameter {@code srcFormat} is
	 *     specified).
	 * </p>
	 *
	 * <p>
	 *     If any of the parsing (time or source pattern) or formatting fails,
	 *     this helper will just return the input {@code time}.
	 * </p>
	 *
	 * @see #formatDateTime(Object, String, DateTimeFormatter, Options)
	 * @param time The time to be formatted
	 * @param pattern The target time format
	 * @param options Handlebars options
	 * @return The input {@code date} formatted with {@code pattern}
	 */
	public Object formatTime(Object time, String pattern, Options options) {
		return formatDateTime(time, pattern, DateTimeFormatter.ISO_TIME, options);
	}

	/**
	 * Format given {@code date} with the specified {@code pattern}.
	 *
	 * <p>
	 *     The input date is expected to be parsable by the specified
	 *     {@code defaultParser} ({@link DateTimeFormatter#ISO_DATE_TIME}),
	 *     unless the optional parameter {@code srcFormat} is specified).
	 * </p>
	 *
	 * <p>
	 *     If any of the parsing (date or source pattern) or formatting fails,
	 *     this helper will just return the input {@code date}.
	 * </p>
	 *
	 * @param dateTime The date or time to be formatted
	 * @param pattern The target date format
	 * @param options Handlebars options
	 * @return The input {@code date} formatted with {@code pattern}
	 */
	private Object formatDateTime(Object dateTime, String pattern, DateTimeFormatter defaultParser, Options options) {
		if (!(dateTime instanceof String)) {
			return dateTime;
		}

		DateTimeFormatter parser = defaultParser == null ? DateTimeFormatter.ISO_DATE_TIME : defaultParser;

		if (options.hash.containsKey(SRC_FORMAT)) {
			Object fmt = options.hash.get(SRC_FORMAT);

			try {
				parser = DateTimeFormatter.ofPattern(fmt.toString());
			} catch (Exception e) {
				log.error("Illegal source date/time format: {}, using default", fmt);
				log.debug("Could not parse date/time format", e);
			}
		}

		TemporalAccessor parsedDate;

		try {
			parsedDate = parser.parse((CharSequence) dateTime);
		} catch (DateTimeParseException e) {
			log.error("Could not parse date/time '{}' with pattern '{}', returning original date/time", dateTime, parser);
			log.debug("Could not parse date/time", e);
			return dateTime;
		}

		try {
			return DateTimeFormatter.ofPattern(pattern).format(parsedDate);
		} catch (Exception e) {
			log.error("Could not format date/time {} with pattern '{}', returning original date/time", parsedDate, pattern);
			log.debug("Could not format date/time", e);
		}

		return dateTime;
	}
}
