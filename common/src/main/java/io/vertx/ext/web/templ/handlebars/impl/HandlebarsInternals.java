package io.vertx.ext.web.templ.handlebars.impl;

import com.github.jknack.handlebars.ValueResolver;

/**
 * A utility class to access various hidden implementations of Handlebars templating engine.
 */
public final class HandlebarsInternals {

	/**
	 * JSON array value resolver.
	 */
	public static final ValueResolver JSON_ARRAY_VALUE_RESOLVER = JsonArrayValueResolver.INSTANCE;

	/**
	 * JSON object value resolver.
	 */
	public static final ValueResolver JSON_OBJECT_VALUE_RESOLVER = JsonObjectValueResolver.INSTANCE;
}
