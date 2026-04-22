package com.gentics.mesh.core.rest.node.field;

import java.util.Objects;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * JSON POJO, common for JSON objects and arrays.
 */
public class JsonContent {

	@JsonIgnore
	private final String jsonString;

	/**
	 * Inner ctor, accepting an already validated json content (array/object) string.
	 * 
	 * @param jsonString
	 */
	protected JsonContent(String jsonString) {
		this.jsonString = jsonString;
	}

	/**
	 * Is this content a JSON array?
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isArray() {
		return StringUtils.isNotBlank(jsonString) && jsonString.startsWith("[") && jsonString.endsWith("]");
	}

	/**
	 * Get either a {@link JsonObject} or {@link JsonArray} or null out of this content.
	 * 
	 * @return
	 */
	@JsonIgnore
	public Object getContent() {
		if (isArray()) {
			return new JsonArray(jsonString);
		} else if (StringUtils.isNotBlank(jsonString)) {
			return new JsonObject(jsonString);
		} else {
			return null;
		}
	}

	/**
	 * Get a {@link JsonObject} of this content, if exists and applicable.
	 * 
	 * @return
	 */
	@JsonIgnore
	public JsonObject getObject() {
		if (isArray() || StringUtils.isBlank(jsonString)) {
			return null;
		} else {
			return new JsonObject(jsonString);
		}
	}

	/**
	 * Get a {@link JsonArray} of this content, if exists and applicable.
	 * 
	 * @return
	 */
	@JsonIgnore
	public JsonArray getArray() {
		if (isArray()) {
			return new JsonArray(jsonString);
		} else {
			return null;
		}
	}

	/**
	 * Utility method for creating a JsonObject from the JSON object.
	 * 
	 * @param object
	 * @return
	 */
	public static JsonContent fromObject(JsonObject object) {
		return new JsonContent(JsonUtil.toJson(object));
	}

	/**
	 * Utility method for creating a JsonObject from the JSON array.
	 * 
	 * @param array
	 * @return
	 */
	public static JsonContent fromArray(JsonArray array) {
		return new JsonContent(JsonUtil.toJson(array));
	}

	/**
	 * Get a string representation of the JSON content.
	 * 
	 * @return
	 */
	@JsonIgnore
	public String getString() {
		return jsonString;
	}

	/**
	 * Utility method for creating a JsonObject from the JSON object/array string representation. May return null.
	 * 
	 * @param jsonString
	 * @return 
	 */
	public static JsonContent fromString(String jsonString) {
		if (StringUtils.isNotBlank(jsonString)) {
			if (jsonString.trim().startsWith("[")) {
				return new JsonContent(JsonUtil.toJson(JsonUtil.readValue(jsonString, JsonArray.class), true));
			} else {
				return new JsonContent(JsonUtil.toJson(JsonUtil.readValue(jsonString, JsonObject.class), true));
			}
		}
		return null;
	}

	@Override
	@JsonIgnore
	public boolean equals(Object obj) {
		if (obj instanceof JsonContent jc) {
			boolean meArray = isArray();
			boolean itArray = jc.isArray();
			if (meArray ^ itArray) {
				return false;
			} else if (meArray) {
				return Objects.equals(getArray(), jc.getArray());
			} else {
				return Objects.equals(getObject(), jc.getObject());
			}
		}
		return false;
	}

	@Override
	@JsonIgnore
	public int hashCode() {
		return isArray() ? getArray().hashCode() : Objects.hashCode(getObject());
	}
}
