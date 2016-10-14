package com.gentics.mesh.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;

/**
 * The {@link FieldMapDeserializer} is used to deserialize the fieldmap within a node response/update request.
 */
public class FieldMapDeserializer extends JsonDeserializer<FieldMap> {

	@Override
	public FieldMap deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		FieldMap map = new FieldMapImpl(node);
		return map;
	}

}
