package com.gentics.mesh.http;

import org.apache.http.entity.ContentType;

import io.vertx.core.http.HttpHeaders;

public final class HttpConstants {

	public static final String LOCATION = HttpHeaders.LOCATION.toString();

	public static final String ETAG = HttpHeaders.ETAG.toString();

	public static final String IF_MATCH = HttpHeaders.IF_MATCH.toString();

	public static final String IF_NONE_MATCH = HttpHeaders.IF_NONE_MATCH.toString();

	public static final String APPLICATION_JSON = ContentType.APPLICATION_JSON.getMimeType();

	public static final String APPLICATION_JSON_UTF8 = ContentType.APPLICATION_JSON.getMimeType() + "; charset=utf-8";

	public static final String APPLICATION_XML = ContentType.APPLICATION_XML.getMimeType();

}
