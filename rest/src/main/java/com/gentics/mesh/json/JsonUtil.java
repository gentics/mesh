package com.gentics.mesh.json;

import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
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
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.ListableField;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;

public final class JsonUtil {


	protected static ObjectMapper mapper;

	protected static ObjectMapper schemaMapper;

	protected static ObjectMapper nodeMapper;
	static {
		initMapper();
		initNodeMapper();
		initSchemaMapper();
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
		module.addDeserializer(Map.class, new FieldMapDeserializer());
		// module.addSerializer(Field.class, new FieldSerializer<Field>());

		nodeMapper.registerModule(new SimpleModule("interfaceMapping") {
			private static final long serialVersionUID = -4667167382238425197L;

			@Override
			public void setupModule(SetupContext context) {
				context.addAbstractTypeResolver(new SimpleAbstractTypeResolver().addMapping(Schema.class, SchemaImpl.class));
			}
		});

		nodeMapper.registerModule(module);
	}

	private static void initMapper() {
		mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public static <T> String toJson(T obj) throws HttpStatusCodeErrorException {
		try {
			// TODO don't use pretty printer in final version
			//return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
			return mapper.writeValueAsString(obj);
		} catch (IOException e) {
			// TODO i18n
			String message = "Could not generate json from object";
			// TODO 500?
			throw new HttpStatusCodeErrorException(500, message, e);
		}
	}

	public static <T> T readNode(String json, Class<T> valueType, final Schema schema) throws IOException, JsonParseException, JsonMappingException {

		InjectableValues values = new InjectableValues() {

			@Override
			public Object findInjectableValue(Object valueId, DeserializationContext ctxt, BeanProperty forProperty, Object beanInstance) {
				if (valueId.toString().equalsIgnoreCase("schema")) {
					return schema;
				} else {
					return null;
				}

			}
		};
		return nodeMapper.reader(values).forType(valueType).readValue(json);
	}

	public static <T> T readValue(String content, Class<T> valueType) throws IOException, JsonParseException, JsonMappingException {
		return mapper.readValue(content, valueType);
	}

	public static <T extends NodeResponse> T readNode(String json, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
		return nodeMapper.readValue(json, clazz);
	}

	public static <T extends Schema> T readSchema(String json) throws JsonParseException, JsonMappingException, IOException {
		return (T) schemaMapper.readValue(json, SchemaImpl.class);
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromJson(RoutingContext rc, Class<?> classOfT) throws HttpStatusCodeErrorException {
		try {
			String body = rc.getBodyAsString();
			return (T) mapper.readValue(body, classOfT);
		} catch (Exception e) {
			//throw new HttpStatusCodeErrorException(400, new I18NService().get(rc, "error_parse_request_json_error"), e);
			throw new HttpStatusCodeErrorException(400, "Error while parsing json.", e);
		}

	}

	public static ObjectMapper getMapper() {
		return mapper;
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

}
