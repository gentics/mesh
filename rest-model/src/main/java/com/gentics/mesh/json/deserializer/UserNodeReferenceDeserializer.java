package com.gentics.mesh.json.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.user.ExpandableNode;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.json.JsonUtil;

/**
 * Deserialize for node references. The deserializer will first try to deserialize the JSON node by passing the JSON to the NodeResponse deserializer. This is
 * done in order to deserialize expanded node references. Second the deserializer will deserialze the JSON node using the basic {@link NodeReference} POJO
 * in order to provide a collapsed form.
 */
public class UserNodeReferenceDeserializer extends JsonDeserializer<ExpandableNode> {

	@Override
	public ExpandableNode deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {

		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		// Only the node reference impl version has a project name property.
		if (node.has("projectName")) {
			NodeReference basicReference = oc.treeToValue(node, NodeReference.class);
			return basicReference;
		} else {
			NodeResponse expandedField = JsonUtil.readValue(node.toString(), NodeResponse.class);
			return expandedField;
		}
	}

}
