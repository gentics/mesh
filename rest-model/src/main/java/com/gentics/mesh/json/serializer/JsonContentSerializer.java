package com.gentics.mesh.json.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.gentics.mesh.core.rest.node.field.JsonContent;

/**
 * Custom JSON serializer for Vert.x {@link JsonContent}.
 */
public class JsonContentSerializer extends JsonSerializer<JsonContent> {

	@Override
	public void serialize(JsonContent value, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
		jgen.writeObject(value.isArray() ? value.getArray() : value.getObject());
	}
}
