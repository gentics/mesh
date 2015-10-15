package com.gentics.mesh.core;

import org.apache.http.entity.ContentType;

public final class HttpConstants {

	public static final String APPLICATION_JSON = ContentType.APPLICATION_JSON.getMimeType();

	public static final String APPLICATION_JSON_UTF8 = ContentType.APPLICATION_JSON.getMimeType() + "; charset=utf-8";

	public static final String APPLICATION_XML = ContentType.APPLICATION_XML.getMimeType();

}
