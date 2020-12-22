package com.gentics.mesh.json.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.vertx.core.json.JsonArray;

/**
 * Custom JSON serializer for Vert.x {@link JsonArray}.
 */
public class JsonArraySerializer extends JsonSerializer<JsonArray> {

	@Override
	public void serialize(JsonArray value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
		jgen.writeObject(value.getList());
	}
}
