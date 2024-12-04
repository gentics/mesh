package com.gentics.mesh.plugin.common.handler;

/**
 * Predefined MIME types for plugin responses.
 *
 * <p>
 *     To avoid problems with registered routes, make sure to use types
 *     <em>without</em> encoding information when restricting a route
 *     with {@link io.vertx.ext.web.Route#consumes(String) consumes()}, but use
 *     types <em>with</em> encoding information for responses.
 * </p>
 */
public interface MimeTypes {

	/** Plain text without encoding information. */
	String TEXT_PLAIN = "text/plain";

	/** Plain text with UTF-8 encoding. */
	String TEXT_PLAIN_UTF8 = "text/plain;charset=UTF-8";

	/** JSON without encoding information. */
	String APPLICATION_JSON = "application/json";

	/** JSON with UTF-8 encoding. */
	String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
}
