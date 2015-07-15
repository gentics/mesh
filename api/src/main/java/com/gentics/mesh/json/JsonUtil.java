package com.gentics.mesh.json;

import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.ListableField;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;

public final class JsonUtil {

	protected static ObjectMapper defaultMapper;

	protected static ObjectMapper schemaMapper;

	protected static ObjectMapper nodeMapper;

	// TODO danger danger - This will cause trouble when doing multithreaded deserialisation!
	protected static Map<String, Object> valuesMap = new HashMap<>();

	static {
		initNodeMapper();
		initSchemaMapper();
		initDefaultMapper();
	}

	private static void initSchemaMapper() {
		schemaMapper = new ObjectMapper();
		schemaMapper.setSerializationInclusion(Include.NON_NULL);
		schemaMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		SimpleModule module = new SimpleModule();
		module.addDeserializer(ListFieldSchema.class, new ListFieldSchemaDeserializer());
		module.addDeserializer(ListableField.class, new FieldDeserializer<ListableField>());
		module.addDeserializer(FieldSchema.class, new FieldSchemaDeserializer<FieldSchema>());
		schemaMapper.registerModule(module);
	}

	private static void initNodeMapper() {
		nodeMapper = new ObjectMapper();
		nodeMapper.setSerializationInclusion(Include.NON_NULL);
		nodeMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		SimpleModule module = new SimpleModule();
		module.addDeserializer(ListableField.class, new FieldDeserializer<ListableField>());
		// module.addDeserializer(MicroschemaListableField.class, new FieldDeserializer<MicroschemaListableField>());
//		module.addDeserializer(NodeResponse.class, new NodeResponseDeserializer());
		module.addDeserializer(Map.class, new FieldMapDeserializer());
		// module.addDeserializer(NodeResponse.class, new NodeResponseDeserializer(nodeMapper, valuesMap));

		nodeMapper.registerModule(new SimpleModule("interfaceMapping") {
			private static final long serialVersionUID = -4667167382238425197L;

			@Override
			public void setupModule(SetupContext context) {
				context.addAbstractTypeResolver(new SimpleAbstractTypeResolver().addMapping(Schema.class, SchemaImpl.class));
			}
		});

		nodeMapper.registerModule(module);
	}

	private static void initDefaultMapper() {
		defaultMapper = new ObjectMapper();
		defaultMapper.setSerializationInclusion(Include.NON_NULL);
		defaultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		SimpleModule module = new SimpleModule();

		module.addSerializer(Field.class, new FieldSerializer<Field>());
		module.addDeserializer(NodeResponse.class, new DelegagingNodeResponseDeserializer<NodeResponse>(nodeMapper, valuesMap, NodeResponse.class));
		module.addDeserializer(NodeCreateRequest.class, new DelegagingNodeResponseDeserializer<NodeCreateRequest>(nodeMapper, valuesMap,
				NodeCreateRequest.class));
		module.addDeserializer(NodeUpdateRequest.class, new DelegagingNodeResponseDeserializer<NodeUpdateRequest>(nodeMapper, valuesMap,
				NodeUpdateRequest.class));
		defaultMapper.registerModule(module);

	}

	public static <T> String toJson(T obj) throws HttpStatusCodeErrorException {
		try {
			// TODO don't use pretty printer in final version
			// return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
			return defaultMapper.writeValueAsString(obj);
		} catch (IOException e) {
			// TODO i18n
			String message = "Could not generate json from object";
			// TODO 500?
			throw new HttpStatusCodeErrorException(500, message, e);
		}
	}

	public static <T> T readNode(String json, Class<T> valueType, SchemaStorage schemaStorage) throws IOException, JsonParseException,
			JsonMappingException {
		valuesMap.put("schema_storage", schemaStorage);
		return defaultMapper.reader(getInjectableValues()).forType(valueType).readValue(json);
	}

	public static InjectableValues getInjectableValues() {

		InjectableValues values = new InjectableValues() {

			@Override
			public Object findInjectableValue(Object valueId, DeserializationContext ctxt, BeanProperty forProperty, Object beanInstance) {
				if (valuesMap.containsKey(valueId.toString())) {
					return valuesMap.get(valueId.toString());
				}
				return null;
			}
		};
		return values;

	}

	public static <T> T readValue(String content, Class<T> valueType) throws IOException, JsonParseException, JsonMappingException {
		return defaultMapper.readValue(content, valueType);
	}

	public static <T> T readSchema(String json, Class<T> classOfT) throws JsonParseException, JsonMappingException, IOException {
		return (T) schemaMapper.readValue(json, classOfT);
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromJson(RoutingContext rc, Class<?> classOfT) throws HttpStatusCodeErrorException {
		try {
			String body = rc.getBodyAsString();
			return (T) defaultMapper.readValue(body, classOfT);
		} catch (Exception e) {
			// throw new HttpStatusCodeErrorException(400, new I18NService().get(rc, "error_parse_request_json_error"), e);
			throw new HttpStatusCodeErrorException(400, "Error while parsing json.", e);
		}

	}

	public static String writeNodeJson(NodeResponse response) {
		try {
			return nodeMapper.writeValueAsString(response);
		} catch (IOException e) {
			// TODO i18n
			String message = "Could not generate json from object";
			// TODO 500?
			throw new HttpStatusCodeErrorException(500, message, e);
		}
	}

	public static ObjectMapper getMapper() {
		return defaultMapper;
	}

	public static ObjectMapper getNodeMapper() {
		return nodeMapper;
	}

}
