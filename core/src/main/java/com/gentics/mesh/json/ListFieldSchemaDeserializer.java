package com.gentics.mesh.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.node.field.ListableField;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;

public class ListFieldSchemaDeserializer extends JsonDeserializer<ListFieldSchema<? extends ListableField>> {

	@Override
	public ListFieldSchema<? extends ListableField> deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();

		System.out.println(node.asText());
		System.out.println(node.get("listType").asText());
		return null;
	}

}
