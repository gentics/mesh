package com.gentics.mesh.plugin.search.util;

import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper functions for building Elasticsearch queries.
 */
public class QueryBuilderUtils {

	private static final Logger log = LoggerFactory.getLogger(QueryBuilderUtils.class);

	/**
	 * Get a regular expression matching all searchable project names.
	 *
	 * @param projects The set of projects to build a regular expression for
	 * @return A regular expression matching all searchable project names.
	 */
	public static String getProjectsRegexp(Set<String> projects) {
		if (projects.contains("*")) {
			return "@";
		}

		return String.join("|", projects);
	}

	/**
	 * Removes HTML markup from he query term parameter <code>q</code>.
	 *
	 * @param request The search request to sanitize
	 * @return The search term parameter <code>q</code> without HTML markup
	 */
	public static String getSanitizedSearchTerm(HttpServerRequest request) {
		String searchTerm = request.getParam("q");
		String sanitized = Jsoup.parse(searchTerm).text();

		log.debug(
			"Sanitizing search term \"{}\": \"{}\"",
			searchTerm,
			sanitized);

		return sanitized;

	}

	/**
	 * Sanitizes the search term parameter <code>q</code> and escapes it, so it
	 * can be used as a value in a JSON object.
	 *
	 * @see #getSanitizedSearchTerm(HttpServerRequest)
	 * @param request The search request to escape
	 * @return The sanitized and JSON escaped search term parameter
	 */
	public static String getEscapedSearchTerm(HttpServerRequest request) {
		String escaped = StringEscapeUtils.escapeJson(getSanitizedSearchTerm(request));

		log.debug("Escaped search term: \"{}\"", escaped);

		return escaped;
	}

	/**
	 * Get the tags from the requests <code>t</code> parameters.
	 *
	 * <p>
	 *     Multiple tags can be specified either by passing multiple <code>t</code> parameters,
	 *     or as a comma separated list. All tags will be merged into a single list.
	 * </p>
	 *
	 * @param request The request to get the tags from
	 * @return The list of tags extraced from the request
	 */
	public static List<String> getTags(HttpServerRequest request) {
		return request.params().getAll("t").stream()
			// Also allow passing comma separated tags with a single t parameter.
			.flatMap(t -> Arrays.stream(t.split(",")))
			.map(t -> StringEscapeUtils.escapeJson(t.trim()))
			.distinct()
			.collect(Collectors.toList());
	}
}
