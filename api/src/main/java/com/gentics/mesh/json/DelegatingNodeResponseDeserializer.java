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

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The deserializer will be used for node responses. We need to determine the schema first before we can deserialize the node. This deserializer will extract
 * the schema information from the json and delegate the deserialization to a different node mapper which will be able to handle the json using the schema
 * information provided.
 * 
 * @param <T>
 */
public class DelegatingNodeResponseDeserializer<T> extends JsonDeserializer<T> {

	private static final Logger log = LoggerFactory.getLogger(DelegatingNodeResponseDeserializer.class);

	private ObjectMapper nodeMapper;
	private Class<T> classOfT;

	private String schemaFieldName = "schema";

	private String schemaInjectableName = "schemaName";

	/**
	 * Create an instance of the deserializer
	 * 
	 * @param nodeMapper node mapper to which the deserialization will be delegated
	 * @param classOfT requested class to deserialize to
	 * @param microschema true if deserialization should be done based on a microschema, false for deserialization based on a schema
	 */
	public DelegatingNodeResponseDeserializer(ObjectMapper nodeMapper, Class<T> classOfT, boolean microschema) {
		this.nodeMapper = nodeMapper;
		this.classOfT = classOfT;
		if (microschema) {
			schemaFieldName = "microschema";
			schemaInjectableName = "microschemaName";
		}
	}

	@Override
	public T deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonNode rootNode = jsonParser.readValueAsTree();
		SchemaStorage schemaStorage = (SchemaStorage) ctxt.findInjectableValue("schema_storage", null, null);

		JsonNode schemaNode = rootNode.get(schemaFieldName);
		if (schemaNode == null) {
			if (log.isDebugEnabled()) {
				log.debug("Error while deserialisation of node json: {" + rootNode.toString() + "}");
			}
			throw new MeshJsonException("The " + schemaFieldName
					+ " reference field could not be found within the json that represents the node. This field is mandatory for deserialisation.");
		}
		// TODO also get schema/microschema by uuid
		String schemaName = schemaNode.get("name").textValue();
		if (StringUtils.isEmpty(schemaName)) {
			throw new MeshJsonException(
					"The " + schemaFieldName + " is empty or not set. This field is mandatory for deserialisation.");
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
				if (schemaInjectableName.equals(valueId.toString())) {
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
