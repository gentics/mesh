package com.gentics.mesh.json;

import io.vertx.core.Future;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.gentics.mesh.core.rest.node.NodeResponse;

public class NodeResponseDeserializer extends StdDeserializer<NodeResponse> implements ResolvableDeserializer {

	private static final long serialVersionUID = 9115584029600249121L;

	private final JsonDeserializer<?> defaultDeserializer;

	public NodeResponseDeserializer(JsonDeserializer<?> defaultDeserializer) {
		super(NodeResponse.class);
		this.defaultDeserializer = defaultDeserializer;
	}

	@Override
	public NodeResponse deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {

		JsonNode node2 = jsonParser.readValueAsTree();
		JsonNode schemaNode = node2.get("schema");
		String schemaName = schemaNode.get("name").textValue();
		ctxt.setAttribute("schemaName", schemaName);

		NodeResponse nodeResponse = (NodeResponse) defaultDeserializer.deserialize(jsonParser, ctxt);
		return nodeResponse;
		//		return super.deserialize(jsonParser, ctxt, new NodeResponse());

		//return jsonParser.readValueAs(NodeResponse.class);

		//		return mapper.convertValue(node, NodeResponse.class);
	}

	//	// for some reason you have to implement ResolvableDeserializer when modifying BeanDeserializer
	//	// otherwise deserializing throws JsonMappingException??
	@Override
	public void resolve(DeserializationContext ctxt) throws JsonMappingException {
		((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
	}

}
