package com.gentics.mesh.plugin.common.handler;

import javax.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;

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
	String TEXT_PLAIN = MediaType.TEXT_PLAIN_TYPE
		.toString();

	/** Plain text with UTF-8 encoding. */
	String TEXT_PLAIN_UTF8 = MediaType.TEXT_PLAIN_TYPE
		.withCharset(StandardCharsets.UTF_8.name())
		.toString();

	/** JSON without encoding information. */
	String APPLICATION_JSON = MediaType.APPLICATION_JSON_TYPE
		.toString();

	/** JSON with UTF-8 encoding. */
	String APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON_TYPE
		.withCharset(StandardCharsets.UTF_8.name())
		.toString();
}
