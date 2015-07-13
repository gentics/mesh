package com.gentics.mesh.json;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DelegagingNodeResponseDeserializer<T> extends JsonDeserializer<T> {

	private ObjectMapper nodeMapper;
	private Map<String, Object> valuesMap;
	private Class<T> classOfT;

	public DelegagingNodeResponseDeserializer(ObjectMapper nodeMapper, Map<String, Object> valuesMap, Class<T> classOfT) {
		this.nodeMapper = nodeMapper;
		this.valuesMap = valuesMap;
		this.classOfT = classOfT;
	}

	@Override
	public T deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonNode rootNode = jsonParser.readValueAsTree();
		JsonNode schemaNode = rootNode.get("schema");
		if (schemaNode == null) {
			throw new IOException(
					"The schema reference field could not be found within the json that represents the node. This field is mandatory for deserialisation.");
		}
		String schemaName = schemaNode.get("name").textValue();
		if (StringUtils.isEmpty(schemaName)) {
			throw new IOException("The schemaName is empty or not set. This field is mandatory for deserialisation.");
		}
		// ctxt.setAttribute("schemaName", schemaName);
		valuesMap.put("schemaName", schemaName);
		//		NodeResponse nodeResponse = nodeMapper.convertValue(node2, NodeResponse.class);
		//		return nodeResponse;
		return nodeMapper.reader(JsonUtil.getInjectableValues()).forType(classOfT).readValue(rootNode.toString());

		// NodeResponse nodeResponse = (NodeResponse) defaultDeserializer.deserialize(jsonParser, ctxt);
		// return super.deserialize(jsonParser, ctxt, new NodeResponse());
		// return jsonParser.readValueAs(NodeResponse.class);
		// return mapper.convertValue(node, NodeResponse.class);
	}
}
