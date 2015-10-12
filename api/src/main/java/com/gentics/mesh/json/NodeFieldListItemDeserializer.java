package com.gentics.mesh.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.SchemaStorage;

/**
 * Deserializer which is used to deserialize node list items.
 *
 */
public class NodeFieldListItemDeserializer extends JsonDeserializer<NodeFieldListItem> {

	@Override
	public NodeFieldListItem deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode jsonNode = oc.readTree(jsonParser);
		SchemaStorage schemaStorage = (SchemaStorage) ctxt.findInjectableValue("schema_storage", null, null);
		return deserialize(jsonNode, jsonParser, schemaStorage);
	}

	public NodeFieldListItem deserialize(JsonNode jsonNode, JsonParser jsonParser, SchemaStorage schemaStorage) throws JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();

		NodeResponse nodeItem = null;
		try {
			// Try to deserialize the node response in the expanded form.
			nodeItem = JsonUtil.readNode(jsonNode.toString(), NodeResponse.class, schemaStorage);
		} catch (MeshJsonException e) {
			// Fallback and deseralize the element using the collapsed form.
			NodeFieldListItemImpl collapsedItem = oc.treeToValue(jsonNode, NodeFieldListItemImpl.class);
			nodeItem = new NodeResponse();
			nodeItem.setUuid(collapsedItem.getUuid());
		} catch (IOException e) {
			throw new MeshJsonException("Could not read node list item.", e);
		}
		return nodeItem;
	}

}
