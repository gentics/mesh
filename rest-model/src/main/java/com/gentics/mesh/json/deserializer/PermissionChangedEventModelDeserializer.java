package com.gentics.mesh.json.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.core.rest.event.role.TagPermissionChangedEventModel;

/**
 * Custom JSON deserializer for {@link PermissionChangedEventModelImpl} events. The implementation uses specific model classes according to the type of the event.
 */
public class PermissionChangedEventModelDeserializer extends JsonDeserializer<PermissionChangedEventModelImpl> {

	@Override
	public PermissionChangedEventModelImpl deserialize(JsonParser jsonParser, DeserializationContext ctxt)
		throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		if (node.get("type") != null) {
			String typeStr = node.get("type").textValue();
			ElementType type = ElementType.valueOf(typeStr);
			switch (type) {
			case TAG:
				return mapper.convertValue(node, TagPermissionChangedEventModel.class);
			case BRANCH:
			case TAGFAMILY:
			case NODE:
				return mapper.convertValue(node, PermissionChangedProjectElementEventModel.class);
			default:
				return mapper.convertValue(node, PermissionChangedEventModelImpl.class);
			}
		}
		return null;
	}
}
