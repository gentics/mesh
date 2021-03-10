package com.gentics.mesh.http;

import io.vertx.core.http.HttpHeaders;

/**
 * List of commonly used http constants
 */
public final class HttpConstants {

	public static final String LOCATION = HttpHeaders.LOCATION.toString();

	public static final String ETAG = HttpHeaders.ETAG.toString();

	public static final String IF_MATCH = HttpHeaders.IF_MATCH.toString();

	public static final String IF_NONE_MATCH = HttpHeaders.IF_NONE_MATCH.toString();

	public static final String APPLICATION_JSON = "application/json";

	public static final String APPLICATION_JSON_UTF8 = APPLICATION_JSON + "; charset=utf-8";

	public static final String APPLICATION_YAML = "application/x-yaml";

	public static final String APPLICATION_YAML_UTF8 = APPLICATION_YAML + "; charset=utf-8";

	public static final String APPLICATION_XML = "application/xml";

}
