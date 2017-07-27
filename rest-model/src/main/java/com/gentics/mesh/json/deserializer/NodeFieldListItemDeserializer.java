package com.gentics.mesh.json.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.json.JsonUtil;

/**
 * Deserializer which is used to deserialize node list items.
 */
public class NodeFieldListItemDeserializer extends JsonDeserializer<NodeFieldListItem> {

	@Override
	public NodeFieldListItem deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode jsonNode = oc.readTree(jsonParser);
		return deserialize(jsonNode, jsonParser);
	}

	/**
	 * Deserialize the node field list item.
	 * 
	 * @param jsonNode
	 *            Node which represents the node field list item.
	 * @param jsonParser
	 * @return Deserialized field list item.
	 * @throws JsonProcessingException
	 */
	public NodeFieldListItem deserialize(JsonNode jsonNode, JsonParser jsonParser) throws JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();

		NodeResponse nodeItem = null;
		try {
			// Try to deserialize the node response in the expanded form.
			nodeItem = JsonUtil.readValue(jsonNode.toString(), NodeResponse.class);
		} catch (GenericRestException e) {
			// Fallback and deseralize the element using the collapsed form.
			NodeFieldListItemImpl collapsedItem = oc.treeToValue(jsonNode, NodeFieldListItemImpl.class);
			nodeItem = new NodeResponse();
			nodeItem.setUuid(collapsedItem.getUuid());
		}
		return nodeItem;
	}

}
