package com.gentics.mesh.json.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.gentics.mesh.core.rest.JsonSchema;

import io.vertx.core.json.JsonObject;

/**
 * Custom serializer for {@link JsonSchema}.
 */
public class JsonSchemaSerializer extends JsonSerializer<JsonSchema> {

	@Override
	public void serialize(JsonSchema value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeObject(new JsonObject(value.toJson()).getMap());
	}
}
