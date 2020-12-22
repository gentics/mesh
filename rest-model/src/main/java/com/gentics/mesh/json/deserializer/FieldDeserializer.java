package com.gentics.mesh.json.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.Field;

/**
 * Custom deserializer for REST node fields. The type of the field selects the specific model class.
 * 
 * @param <T>
 */
public class FieldDeserializer<T extends Field> extends JsonDeserializer<T> {

	@Override
	public T deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		if (node.get("type") != null) {
			String type = node.get("type").textValue();
			FieldTypes ft = FieldTypes.valueByName(type);
			return (T) mapper.convertValue(node, ft.getFieldImplementationClass());
		}
		return null;
	}
}
