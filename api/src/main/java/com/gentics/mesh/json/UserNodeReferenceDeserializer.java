package com.gentics.mesh.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UserNodeReferenceDeserializer extends JsonDeserializer<NodeReference> {

	private static final Logger log = LoggerFactory.getLogger(UserNodeReferenceDeserializer.class);

	@Override
	public NodeReference deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {

		SchemaStorage schemaStorage = (SchemaStorage) ctxt.findInjectableValue("schema_storage", null, null);
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		// Try to deserialize the field in the expanded version
		try {
			NodeResponse expandedField = JsonUtil.readNode(node.toString(), NodeResponse.class, schemaStorage);
			return expandedField;
		} catch (MeshJsonException e) {
			if (log.isTraceEnabled()) {
				log.trace("Could not deserialize json to expanded Node Response. I'll try to fallback to a collapsed version.", e);
			}
			NodeReferenceImpl basicReference = oc.treeToValue(node, NodeReferenceImpl.class);
			return basicReference;
			//			NodeResponse restNode = new NodeResponse();
			//			restNode.setUuid(collapsedField.getUuid());
			//			map.put(fieldKey, restNode);
		} catch (IOException e) {
			throw new MeshJsonException("Could not read node reference", e);
		}
	}

}
