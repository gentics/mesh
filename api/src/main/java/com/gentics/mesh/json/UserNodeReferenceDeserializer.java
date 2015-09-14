package com.gentics.mesh.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.gentics.mesh.core.rest.user.NodeReference;

public class UserNodeReferenceDeserializer extends JsonDeserializer<NodeReference> {

	@Override
	public NodeReference deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		// TODO Auto-generated method stub
		return null;
	}

}
