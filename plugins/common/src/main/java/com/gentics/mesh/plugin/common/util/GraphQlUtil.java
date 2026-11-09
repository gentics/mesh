package com.gentics.mesh.plugin.common.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

/**
 * Miscellaneous utility methods for handling results of GraphQL queries.
 */
public class GraphQlUtil {

	private static final String FIELDS = "fields";

	/**
	 * Get the specified field from the node.
	 *
	 * @param node The node to get the field from
	 * @param field The name of the field to get
	 * @param <T> The type of the field
	 * @return The specified field from the node
	 */
	public static <T> T getField(JsonObject node, String field) {
		return (T) node.getJsonObject(FIELDS).getValue(field);
	}

	/**
	 * Extract the children from the given GraphQL query result.
	 *
	 * <p>
	 *     If a child object contains the {@code fields} field, all the
	 *     elements from the {@code fields} are moved up one level (directly to
	 *     the child object).
	 * </p>
	 *
	 * @param result The result to extract children from
	 * @return The {@link JsonArray} containing the children encoded as String
	 */
	public static String childrenExtractor(JsonObject result) {
		return getChildrenFromResult(result).stream()
			.map(o -> (JsonObject) o)
			.map(o -> {
				if (o.containsKey(FIELDS)) {
					o.getJsonObject(FIELDS).forEach(e -> o.put(e.getKey(), e.getValue()));
					o.remove(FIELDS);
				}

				return o;
			})
			.collect(JsonArray::new, JsonArray::add, JsonArray::addAll)
			.encode();
	}

	/**
	 * Extract the children from the given GraphQL query result.
	 *
	 * <p>
	 *     The children are extracted as is without any further modifications.
	 * </p>
	 *
	 * @param result The result to extract children from
	 * @return The {@link JsonArray} containing the children encoded as String
	 */
	public static String rawChildrenExtractor(JsonObject result) {
		return getChildrenFromResult(result).encode();
	}

	/**
	 * Resolve the children elements of the GraphQL result.
	 *
	 * @param result The GrapqhQL result
	 * @return The {@code JsonArray} containing the children of the result node
	 */
	public static JsonArray getChildrenFromResult(JsonObject result) {
		return Optional.ofNullable(result)
			.map(o -> o.getJsonObject("node"))
			.map(o -> o.getJsonObject("children"))
			.map(o -> o.getJsonArray("elements"))
			.orElse(new JsonArray());
	}
}
