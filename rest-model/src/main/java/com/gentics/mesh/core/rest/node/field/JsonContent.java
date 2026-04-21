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
	private String jsonString;

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
	 * Set the JSON object as content.
	 * 
	 * @param object
	 * @return
	 */
	@JsonIgnore
	public JsonContent setObject(JsonObject object) {
		this.jsonString = JsonUtil.toJson(object);
		return this;
	}

	/**
	 * Set the JSON array as content
	 * 
	 * @param array
	 * @return
	 */
	@JsonIgnore
	public JsonContent setArray(JsonArray array) {
		this.jsonString = JsonUtil.toJson(array);
		return this;
	}

	/**
	 * Get a string representation of the JSON content
	 * 
	 * @return
	 */
	@JsonIgnore
	public String getString() {
		return jsonString;
	}

	/**
	 * Set a JSON content via its string representation.
	 * 
	 * @param jsonString
	 * @return 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JsonContent setString(String jsonString) {
		this.jsonString = StringUtils.isNotBlank(jsonString) ? (
					JsonUtil.toJson(JsonUtil.readValue(jsonString, jsonString.trim().startsWith("[") ? (Class) JsonArray.class : JsonObject.class), true)
				) : jsonString;
		return this;
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
