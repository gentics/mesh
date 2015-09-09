package com.gentics.mesh.json;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;

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
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.ListableField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;

public final class JsonUtil {

	protected static ObjectMapper defaultMapper;

	protected static ObjectMapper schemaMapper;

	protected static ObjectMapper nodeMapper;

	public static boolean debugMode = false;

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
		//		module.addDeserializer(ListFieldSchema.class, new ListFieldSchemaDeserializer());
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
		module.addDeserializer(FieldMap.class, new FieldMapDeserializer());

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
		module.addDeserializer(NodeFieldListItem.class, new NodeFieldListItemDeserializer());
		module.addSerializer(NumberFieldImpl.class, new BasicFieldSerializer<NumberFieldImpl>());
		module.addSerializer(HtmlFieldImpl.class, new BasicFieldSerializer<HtmlFieldImpl>());
		module.addSerializer(StringFieldImpl.class, new BasicFieldSerializer<StringFieldImpl>());
		module.addSerializer(DateFieldImpl.class, new BasicFieldSerializer<DateFieldImpl>());
		module.addSerializer(BooleanFieldImpl.class, new BasicFieldSerializer<BooleanFieldImpl>());
		module.addSerializer(FieldList.class, new FieldListSerializer());

		module.addDeserializer(NodeResponse.class, new DelegagingNodeResponseDeserializer<NodeResponse>(nodeMapper, NodeResponse.class));
		module.addDeserializer(NodeCreateRequest.class,
				new DelegagingNodeResponseDeserializer<NodeCreateRequest>(nodeMapper, NodeCreateRequest.class));
		module.addDeserializer(NodeUpdateRequest.class,
				new DelegagingNodeResponseDeserializer<NodeUpdateRequest>(nodeMapper, NodeUpdateRequest.class));
		defaultMapper.registerModule(module);

	}

	public static <T> String toJson(T obj) throws HttpStatusCodeErrorException {
		try {
			if (debugMode) {
				return defaultMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
			} else {
				return defaultMapper.writeValueAsString(obj);
			}
		} catch (IOException e) {
			// TODO i18n
			String message = "Could not generate json from object";
			// TODO 500?
			throw new HttpStatusCodeErrorException(INTERNAL_SERVER_ERROR, message, e);
		}
	}

	public static <T> T readNode(String json, Class<T> valueType, SchemaStorage schemaStorage)
			throws IOException, JsonParseException, JsonMappingException {

		InjectableValues injectedSchemaStorage = new InjectableValues() {

			@Override
			public Object findInjectableValue(Object valueId, DeserializationContext ctxt, BeanProperty forProperty, Object beanInstance) {
				if ("schema_storage".equals(valueId.toString())) {
					return schemaStorage;
				}
				return null;
			}
		};

		return defaultMapper.reader(injectedSchemaStorage).forType(valueType).readValue(json);
	}

	public static <T> T readValue(String content, Class<T> valueType) throws IOException, JsonParseException, JsonMappingException {
		return defaultMapper.readValue(content, valueType);
	}

	public static <T> T readSchema(String json, Class<T> classOfT) throws JsonParseException, JsonMappingException, IOException {
		return (T) schemaMapper.readValue(json, classOfT);
	}

	public static String writeNodeJson(NodeResponse response) {
		try {
			return nodeMapper.writeValueAsString(response);
		} catch (IOException e) {
			// TODO i18n
			String message = "Could not generate json from object";
			// TODO 500?
			throw new HttpStatusCodeErrorException(INTERNAL_SERVER_ERROR, message, e);
		}
	}

	public static ObjectMapper getMapper() {
		return defaultMapper;
	}

	public static ObjectMapper getNodeMapper() {
		return nodeMapper;
	}

}
