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
	
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	public static final String MULTIPART_FORM_DATA = "multipart/form-data";

	public static final String TEXT_PLAIN = "text/plain";

	public static final String TEXT_PLAIN_UTF8 = TEXT_PLAIN + "; charset=utf-8";

	public static final String TEXT_HTML = "text/html";

	public static final String TEXT_HTML_UTF8 = TEXT_HTML + "; charset=utf-8";

	public static final String APPLICATION_JSON_UTF8 = APPLICATION_JSON + "; charset=utf-8";

	public static final String APPLICATION_YAML = "application/x-yaml";

	public static final String APPLICATION_YAML_UTF8 = APPLICATION_YAML + "; charset=utf-8";

	public static final String APPLICATION_XML = "application/xml";

}
