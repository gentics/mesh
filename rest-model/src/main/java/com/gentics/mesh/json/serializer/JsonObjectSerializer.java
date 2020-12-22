package com.gentics.mesh.json.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.vertx.core.json.JsonObject;

/**
 * Custom serializer for Vert.x {@link JsonObject}
 */
public class JsonObjectSerializer extends JsonSerializer<JsonObject> {

	@Override
	public void serialize(JsonObject value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
		jgen.writeObject(value.getMap());
	}
}
