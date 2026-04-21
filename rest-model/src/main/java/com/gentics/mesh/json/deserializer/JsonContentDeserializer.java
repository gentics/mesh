package com.gentics.mesh.json.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.node.field.JsonContent;

public class JsonContentDeserializer extends JsonDeserializer<JsonContent> {

	@Override
	public JsonContent deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);
		return new JsonContent().setString(node.toString());
	}

}
