package com.gentics.mesh.json;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.schema.SchemaStorage;

/**
 * The deserializer will be used for node responses. We need to determine the schema first before we can deserialize the node. This deserializer will extract
 * the schema information from the json and delegate the deserialization to a different node mapper which will be able to handle the json using the schema
 * information provided.
 * 
 * @author johannes2
 *
 * @param <T>
 */
public class DelegagingNodeResponseDeserializer<T> extends JsonDeserializer<T> {

	private ObjectMapper nodeMapper;
	private Class<T> classOfT;

	public DelegagingNodeResponseDeserializer(ObjectMapper nodeMapper, Class<T> classOfT) {
		this.nodeMapper = nodeMapper;
		this.classOfT = classOfT;
	}

	@Override
	public T deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonNode rootNode = jsonParser.readValueAsTree();
		SchemaStorage schemaStorage = (SchemaStorage) ctxt.findInjectableValue("schema_storage", null, null);

		JsonNode schemaNode = rootNode.get("schema");
		if (schemaNode == null) {
			throw new MeshJsonException(
					"The schema reference field could not be found within the json that represents the node. This field is mandatory for deserialisation.");
		}
		String schemaName = schemaNode.get("name").textValue();
		if (StringUtils.isEmpty(schemaName)) {
			throw new MeshJsonException("The schemaName is empty or not set. This field is mandatory for deserialisation.");
		}
		// NodeResponse nodeResponse = nodeMapper.convertValue(node2, NodeResponse.class);
		// return nodeResponse;
		return nodeMapper.reader(getInjectableValueWithSchemaName(schemaName, schemaStorage)).forType(classOfT).readValue(rootNode.toString());

		// NodeResponse nodeResponse = (NodeResponse) defaultDeserializer.deserialize(jsonParser, ctxt);
		// return super.deserialize(jsonParser, ctxt, new NodeResponse());
		// return jsonParser.readValueAs(NodeResponse.class);
		// return mapper.convertValue(node, NodeResponse.class);
	}

	private InjectableValues getInjectableValueWithSchemaName(String schemaName, SchemaStorage schemaStorage) {

		InjectableValues values = new InjectableValues() {

			@Override
			public Object findInjectableValue(Object valueId, DeserializationContext ctxt, BeanProperty forProperty, Object beanInstance) {
				if ("schemaName".equals(valueId.toString())) {
					return schemaName;
				}
				if ("schema_storage".equals(valueId.toString())) {
					return schemaStorage;
				}
				return null;
			}
		};
		return values;

	}
}
