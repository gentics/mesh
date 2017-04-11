package com.gentics.mesh.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The field schema deserialize will deserialize the field by first examining the field type and delegating the deserialization to another jackson mapper which
 * will deserialize the node using the appropriate schema implementation class.
 *
 * @param <T>
 */
public class FieldSchemaDeserializer<T extends FieldSchema> extends JsonDeserializer<T> {

	private static final Logger log = LoggerFactory.getLogger(FieldSchemaDeserializer.class);

	@Override
	public T deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		if (node.get("type") != null) {
			String type = node.get("type")
					.textValue();
			FieldTypes ft = FieldTypes.valueByName(type);
			return (T) mapper.convertValue(node, ft.getSchemaImplementationClazz());
		} else {
			JsonNode nameField = node.get("name");
			String fieldName = "unknown";
			if (nameField != null) {
				fieldName = nameField.textValue();
			}
			log.error("Schema type not found for field with name {" + fieldName + "}");
			throw new JsonMappingException("Missing type property for field {" + fieldName + "}", jsonParser.getCurrentLocation());
		}
	}
}
