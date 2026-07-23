package com.gentics.mesh.core.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonObject;

/**
 * A wrapper of serializable {@link JsonObject} to produce {@link io.vertx.reactivex.json.schema.JsonSchema}s out of it.
 */
public interface JsonSchema extends RestModel {

	/**
	 * Get the type of this schema.
	 * 
	 * @return
	 */
	String getType();

	/**
	 * Get Vert.x type of this schema.
	 * 
	 * @return
	 */
	@JsonIgnore
	default io.vertx.reactivex.json.schema.JsonSchema getVertxSchema() {
		return io.vertx.reactivex.json.schema.JsonSchema.of(new JsonObject(toJson()));
	}

	/**
	 * Parse provided JsonObject as JsonSchema. Returns null if invalid.
	 * 
	 * @param object
	 * @return
	 */
	@JsonIgnore
	static JsonSchema from(JsonObject object) {
		String type = (object == null || object.getString("type") == null) ? "object" : object.getString("type");
		switch (type) {
		case "object":
			Map<String, JsonSchemaType> properties = (object == null || object.getJsonObject("properties") == null) 
					? new HashMap<>() 
					: object.getJsonObject("properties").getMap().entrySet().stream()
							.map(e -> Pair.of(e.getKey(), JsonUtil.readValue(JsonUtil.toJson(e.getValue()), JsonSchemaType.class)))
							.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
			String[] required = (object == null || object.getJsonArray("required") == null) 
					? new String[0] 
					: object.getJsonArray("required").stream().map(Object::toString).toArray(size -> new String[size]);
			return new JsonObjectSchema(required, properties);
		case "array":
			JsonSchemaType items = (object != null && object.getJsonObject("items") != null)
					? JsonUtil.readValue(JsonUtil.toJson(object.getJsonObject("items")), JsonSchemaType.class)
					: null;
			return new JsonArraySchema(items);
		}

		return null;
	}

	/**
	 * Parse provided JSON string as JsonSchema. Returns null if invalid.
	 * 
	 * @param json
	 * @return
	 */
	@JsonIgnore
	static JsonSchema from(String json) {
		return json == null ? null : from(new JsonObject(json));
	}
}
