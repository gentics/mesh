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
public class JsonSchema extends JsonSchemaType {

	private String[] required = new String[0];
	private Map<String, JsonSchemaType> properties = new HashMap<>();

	
	public JsonSchema() {
		super();
	}

	public JsonSchema(JsonObject object) {
		super((object == null || object.getString("type") == null) ? "object" : object.getString("type"));
		setRequired((object == null || object.getJsonArray("required") == null) 
				? new String[0] 
				: object.getJsonArray("required").stream().map(Object::toString).toArray(size -> new String[size]));
		setProperties((object == null || object.getJsonObject("properties") == null) 
				? new HashMap<>() 
				: object.getJsonObject("properties").getMap().entrySet().stream()
						.map(e -> Pair.of(e.getKey(), JsonUtil.readValue(JsonUtil.toJson(e.getValue()), JsonSchemaType.class)))
						.collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
	}
	public JsonSchema(String json) {
		this(json == null ? null : new JsonObject(json));
	}

	@Override
	public JsonSchema setType(String type) {
		super.setType(type);
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof RestModel) ? JsonUtil.equals(this, (RestModel) obj) : false;
	}

	@Override
	public int hashCode() {
		return JsonUtil.toJson(this).hashCode();
	}

	@JsonIgnore
	public io.vertx.reactivex.json.schema.JsonSchema getVertxSchema() {
		return io.vertx.reactivex.json.schema.JsonSchema.of(new JsonObject(toJson()));
	}

	public String[] getRequired() {
		return required;
	}

	public JsonSchema setRequired(String[] required) {
		this.required = required;
		return this;
	}

	public Map<String, JsonSchemaType> getProperties() {
		return properties;
	}

	public JsonSchema setProperties(Map<String, JsonSchemaType> properties) {
		this.properties = properties;
		return this;
	}
}
