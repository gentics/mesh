package com.gentics.mesh.plugins.common.handlebars;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import com.github.jknack.handlebars.ValueResolver;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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

	/**
	 * @author <a href="https://github.com/Jotschi">Johannes Schüth</a>
	 */
	static class JsonArrayValueResolver implements ValueResolver {

		public static final ValueResolver INSTANCE = new JsonArrayValueResolver();

		@Override
		public Object resolve(final Object context) {
			if (context instanceof JsonArray) {
				return context;
			}
			return UNRESOLVED;
		}

		@Override
		public Object resolve(Object context, String name) {
			if (context instanceof JsonArray) {
				JsonArray jsonArray = ((JsonArray) context);
				if ("length".equals(name) || "size".equals(name)) {
					return jsonArray.size();
				}
				// NumberFormatException will bubble up and cause a HandlebarsException with
				// line, row info
				Object value = jsonArray.getValue(Integer.parseInt(name));
				if (value != null) {
					return value;
				}
			}
			return UNRESOLVED;
		}

		@Override
		public Set<Entry<String, Object>> propertySet(final Object context) {
			return Collections.emptySet();
		}
	}

	/**
	 * @author <a href="https://github.com/Jotschi">Johannes Schüth</a>
	 */
	static class JsonObjectValueResolver implements ValueResolver {

		public static final ValueResolver INSTANCE = new JsonObjectValueResolver();

		@Override
		public Object resolve(final Object context, final String name) {
			Object value = null;
			if (context instanceof JsonObject) {
				value = ((JsonObject) context).getValue(name);
			}
			return value == null ? UNRESOLVED : value;
		}

		@Override
		public Object resolve(final Object context) {
			if (context instanceof JsonObject) {
				return context;
			}
			return UNRESOLVED;
		}

		@Override
		public Set<Entry<String, Object>> propertySet(final Object context) {
			if (context instanceof JsonObject) {
				return ((JsonObject) context).getMap().entrySet();
			}
			return Collections.emptySet();
		}
	}
}
