package com.gentics.mesh.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;

import java.io.IOException;

/**
 * Custom deserializer for event cause information.
 */
public class EventCauseInfoDeserializer extends JsonDeserializer<EventCauseInfo> {

	@Override
	public EventCauseInfo deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		if (node.get("action") != null) {
			String type = node.get("action").textValue();
			EventCauseAction action = EventCauseAction.valueOf(type);
			Class<EventCauseInfo> modelClass = action.getModelClass();
			// No class defined. Cause can't be deserialized for the given action
			if (modelClass == null) {
				return null;
			}
			return mapper.convertValue(node, modelClass);
		}
		return null;
	}
}
