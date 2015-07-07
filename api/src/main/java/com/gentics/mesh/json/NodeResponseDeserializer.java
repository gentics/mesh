package com.gentics.mesh.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.node.NodeResponse;

public class NodeResponseDeserializer extends JsonDeserializer<NodeResponse> {

	private ObjectMapper nodeMapper;
	private Map<String, Object> valuesMap;

	public NodeResponseDeserializer(ObjectMapper nodeMapper, Map<String, Object> valuesMap) {
		this.nodeMapper = nodeMapper;
		this.valuesMap = valuesMap;
	}

	@Override
	public NodeResponse deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonNode node2 = jsonParser.readValueAsTree();
		// ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();

		JsonNode schemaNode = node2.get("schema");
		String schemaName = schemaNode.get("name").textValue();
		// ctxt.setAttribute("schemaName", schemaName);
		valuesMap.put("schemaName", schemaName);
//		NodeResponse nodeResponse = nodeMapper.convertValue(node2, NodeResponse.class);
//		return nodeResponse;
		return nodeMapper.reader(JsonUtil.getInjectableValues()).forType(NodeResponse.class).readValue(node2.toString());

		// NodeResponse nodeResponse = (NodeResponse) defaultDeserializer.deserialize(jsonParser, ctxt);
		// return super.deserialize(jsonParser, ctxt, new NodeResponse());

		// return jsonParser.readValueAs(NodeResponse.class);

		// return mapper.convertValue(node, NodeResponse.class);
	}
}
