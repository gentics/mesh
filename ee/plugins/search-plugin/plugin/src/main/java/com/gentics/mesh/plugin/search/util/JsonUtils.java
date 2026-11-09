package com.gentics.mesh.plugin.search.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Utility methods for working with {@link JsonObject JSON objects}.
 */
public class JsonUtils {

	private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
	private static final ObjectMapper jacksonMapper = new ObjectMapper();

	/**
	 * Count the number of entries in a JSON object or array.
	 *
	 * @param obj The object to count fields in
	 * @return The number of entries in the given JSON object or array
	 */
	public static int countEntries(Object obj) {
		Iterator<Object> values;

		if (obj instanceof JsonObject) {
			values = ((JsonObject) obj).stream().map(Entry::getValue).collect(Collectors.toList()).iterator();
		} else if (obj instanceof JsonArray) {
			values = ((JsonArray) obj).iterator();
		} else {
			return 1;
		}

		int count = 0;

		while (values.hasNext()) {
			count += countEntries(values.next());
		}

		return count;
	}

	/**
	 * Create a {@link JsonObject} from the source string even if it
	 * is actually a {@link JsonArray}.
	 *
	 * In case the input is an array, the single field <code>elements</code>
	 * will contain the actual data.
	 *
	 * @param str The JSON source string
	 * @return The corresponding JsonObject
	 * @throws DecodeException When the source string is not valid JSON
	 */
	public static JsonObject jsonObjectOf(String str) throws DecodeException {
		if (str.charAt(0) == '[') {
			return new JsonObject().put("elements", new JsonArray(str));
		}

		return new JsonObject(str);
	}

	/**
	 * Create a {@link JsonObject} from the source buffer even if it
	 * actually contains a {@link JsonArray}.
	 *
	 * In case the input is an array, the single field <code>elements</code>
	 * will contain the actual data.
	 *
	 * @param buf The Buffer containing the source string
	 * @return The corresponding JsonObject
	 * @throws DecodeException When the source string is not valid JSON
	 */
	public static JsonObject jsonObjectOf(Buffer buf) throws DecodeException {
		if ("[".equals(buf.getString(0, 1))) {
			return new JsonObject().put("elements", buf.toJsonArray());
		}

		return buf.toJsonObject();
	}

	/**
	 * Convert the jackson {@link ObjectNode} to a Vert.x {@link JsonObject}
	 *
	 * <p>
	 * The input object is serialized with the {@link #jacksonMapper object mapper} and the resulting
	 * string is used to create a {@link JsonObject Vert.x JSON object}.
	 * </p>
	 *
	 * @param json The jackson JSON object
	 * @return The converted Vert.x JSON object
	 * @throws RuntimeException When the jackson object mapper could not serialize the input object
	 * @throws DecodeException When the string returned by the object mapper is not valid JSON
	 */
	public static JsonObject jsonObjectOf(ObjectNode json) throws DecodeException {
		try {
			return jsonObjectOf(jacksonMapper.writeValueAsString(json));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the value stored under the specified path from the JSON object.
	 *
	 * <p>
	 * No checks on the structure of the JSON object are made, when any but
	 * the last element in <code>path</code> is not a JSON object field of
	 * its parent, or if the path is <code>null</code> the
	 * <code>devaultValue</code> will be returned.
	 * </p>
	 *
	 * @see #getValue(JsonObject, String[], Object)
	 *
	 * @param obj The JSON object to get a value from
	 * @param path The path to the value
	 * @param defaultValue The default value to return
	 *
	 * @return The value stored under the specified path in the given JSON object
	 */
	public static <T> T getValue(JsonObject obj, String path, T defaultValue) {
		if (obj == null) {
			log.debug("No JSON object provided");

			return defaultValue;
		}

		if (path == null || path.isEmpty()) {
			log.warn("No path provided");

			return defaultValue;
		}

		return getValue(obj, path.split("/"), defaultValue);
	}

	/**
	 * Get the value stored under the specified path from the JSON object.
	 *
	 * <p>
	 * No checks on the structure of the JSON object are made, when any but
	 * the last element in <code>path</code> is not a JSON object field of
	 * its parent, or if the path is <code>null</code> the
	 * <code>devaultValue</code> will be returned.
	 * </p>
	 *
	 * @param obj The JSON object to get a value from
	 * @param pathElements The path to the value
	 * @param defaultValue The default value to return
	 *
	 * @return The value stored under the specified path in the given JSON object
	 */
	@SuppressWarnings("unchecked")
	private static <T> T getValue(JsonObject obj, String[] pathElements, T defaultValue) {
		if (log.isDebugEnabled()) {
			log.debug("Resolving path {} in\n{}", String.join(".", pathElements), obj);
		}

		int lastIdx = pathElements.length - 1;

		for (int ii = 0; ii < lastIdx; ii++) {
			boolean error = true;

			try {
				obj = resolveField(obj, pathElements[ii]);
				error = obj == null;
			} catch (Exception e) {
				log.error("Could not get element " + pathElements[ii] + " of path " + Arrays.toString(pathElements), e);
			}
			if (error) {
				log.debug("Value for field \"{}\" ({}) is not a JsonObject", pathElements[ii], ii);
				return defaultValue;
			}
		}

		try {
			Object val = resolveField(obj, pathElements[lastIdx]);
			return val == null ? defaultValue : (T) val;
		} catch (ClassCastException e) {
			log.error("Unexpected field type", e);
		}
		return defaultValue;
	}

	/**
	 * Set the value for the specified path.
	 *
	 * <p>
	 *	 <strong>NOTE</strong> that setting values in {@link JsonArray JsonArrays} is not supported.
	 * </p>
	 *
	 * @param obj The JSON object to set the value in
	 * @param path The path in the JSON object to set the value
	 * @param value The value to set
	 * @return The old value stored under <code>path</code>
	 */
	public static Object setValue(JsonObject obj, String path, Object value) {
		if (obj == null) {
			log.debug("No JSON object provided");
			return null;
		}
		if (path == null || path.isEmpty()) {
			log.warn("No path provided");
			return null;
		}
		String[] pathElements = path.split("/");
		String fieldName = pathElements[pathElements.length - 1];

		pathElements = (String[]) ArrayUtils.subarray(pathElements, 0, pathElements.length - 1);
		JsonObject target = getValue(obj, pathElements, null);

		if (target == null) {
			return null;
		}
		Object oldVal = target.getValue(fieldName);
		target.put(fieldName, value);
		return oldVal;
	}

	/**
	 * Resolve the value of <code>field</code> in <code>obj</code>.
	 *
	 * <p>
	 *	 When <code>field</code> is a normal field name, it is just retrieved via
	 *	 {@link JsonObject#getValue(String)}. When the field is an array access,
	 *	 the respective entry in the {@link JsonArray} will be retrieved.
	 * </p>
	 *
	 * @param <T> The type of the field
	 * @param obj The <code>JsonObject</code> to get the field from
	 * @param field A normal field name, or a field name with array access
	 * @return The value of the resolved field in <code>obj</code>
	 */
	@SuppressWarnings("unchecked")
	private static <T> T resolveField(JsonObject obj, String field) {
		int bracketIdx = field.indexOf('[');

		if (bracketIdx < 0) {
			return (T) obj.getValue(field);
		}

		String[] indices = field.substring(bracketIdx + 1, field.length() - 1).split("\\]\\[");
		JsonArray array = obj.getJsonArray(field.substring(0, bracketIdx));
		int lastIdx = indices.length - 1;

		for (int ii = 0; ii < lastIdx; ii++) {
			try {
				array = array.getJsonArray(Integer.parseInt(indices[ii]));
			} catch (Exception e) {
				log.error("Could not resolve element for index " + indices[ii] + " at position " + ii, e);
				return null;
			}
		}

		T ret;
		try {
			ret = (T) array.getValue(Integer.parseInt(indices[lastIdx]));
		} catch (Exception e) {
			log.error("Could not resolve element for last index " + indices[lastIdx], e);
			ret = null;
		}
		return ret;
	}
}
