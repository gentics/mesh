package com.gentics.mesh.json.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.error.AbstractRestException;
import com.gentics.mesh.core.rest.error.Errors;
import com.gentics.mesh.json.MeshJsonException;

/**
 * Custom deserializer for REST exceptions.
 */
public class RestExceptionDeserializer extends JsonDeserializer<AbstractRestException> {

	@Override
	public AbstractRestException deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		if (node.get("type") != null) {
			String type = node.get("type")
					.textValue();
			Errors et = Errors.valueByName(type);
			if (et == null) {
				throw new MeshJsonException("Invalid error type {" + type + "}");
			}
			return (AbstractRestException) mapper.convertValue(node, et.getClazz());
		}
		return null;
	}

}
