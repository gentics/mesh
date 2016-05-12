package com.gentics.mesh.json;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gentics.mesh.core.rest.error.AbstractRestException;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapJsonImpl;
import com.gentics.mesh.core.rest.node.field.ListableField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.rest.user.NodeReference;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Main JSON Util which is used to register all custom JSON specific handlers and deserializers.
 *
 */
public final class JsonUtil {

	protected static ObjectMapper defaultMapper;

	private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

	/**
	 * When enabled indented JSON will be produced.
	 */
	public static boolean debugMode = false;

	static {
		initDefaultMapper();
	}

	/**
	 * Initialize the default mapper.
	 */
	private static void initDefaultMapper() {
		defaultMapper = new ObjectMapper();
		defaultMapper.setSerializationInclusion(Include.NON_NULL);
		defaultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		SimpleModule module = new SimpleModule();
		module.addDeserializer(AbstractRestException.class, new RestExceptionDeserializer());
		module.addDeserializer(NodeFieldListItem.class, new NodeFieldListItemDeserializer());
		module.addSerializer(NumberFieldImpl.class, new BasicFieldSerializer<NumberFieldImpl>());
		module.addSerializer(HtmlFieldImpl.class, new BasicFieldSerializer<HtmlFieldImpl>());
		module.addSerializer(StringFieldImpl.class, new BasicFieldSerializer<StringFieldImpl>());
		module.addSerializer(DateFieldImpl.class, new BasicFieldSerializer<DateFieldImpl>());
		module.addSerializer(BooleanFieldImpl.class, new BasicFieldSerializer<BooleanFieldImpl>());
		module.addSerializer(FieldList.class, new FieldListSerializer());
		module.addSerializer(FieldMapJsonImpl.class, new JsonSerializer<FieldMapJsonImpl>() {
			@Override
			public void serialize(FieldMapJsonImpl value, JsonGenerator gen, SerializerProvider serializers)
					throws IOException, JsonProcessingException {
				gen.writeObject(value.getNode());
			}
		});

		module.addDeserializer(FieldMap.class, new FieldMapDeserializer());
		module.addDeserializer(NodeReference.class, new UserNodeReferenceDeserializer());
		module.addDeserializer(ListableField.class, new FieldDeserializer<ListableField>());
		module.addDeserializer(FieldSchema.class, new FieldSchemaDeserializer<FieldSchema>());

		defaultMapper.registerModule(module);

		defaultMapper.registerModule(new SimpleModule("interfaceMapping") {
			private static final long serialVersionUID = -4667167382238425197L;

			@Override
			public void setupModule(SetupContext context) {
				context.addAbstractTypeResolver(new SimpleAbstractTypeResolver().addMapping(Schema.class, SchemaModel.class));
				context.addAbstractTypeResolver(new SimpleAbstractTypeResolver().addMapping(Microschema.class, MicroschemaModel.class));
			}
		});

	}

	public static <T> String toJson(T obj) throws GenericRestException {
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
			throw new GenericRestException(INTERNAL_SERVER_ERROR, message, e);
		}
	}

	public static <T> T readValue(String content, Class<T> valueType) throws IOException {
		try {
			return defaultMapper.readValue(content, valueType);
		} catch (JsonMappingException e) {
			log.error("Could not deserialize json {" + content + "} into {" + valueType.getName() + "}", e);
			String line = "unknown";
			String column = "unknown";
			if (e.getLocation() != null) {
				line = String.valueOf(e.getLocation().getLineNr());
				column = String.valueOf(e.getLocation().getColumnNr());
			}
			String field = "";
			if (e.getPath() != null && e.getPath().size() >= 1) {
				field = e.getPath().get(0).getFieldName();
			}
			throw new GenericRestException(BAD_REQUEST, "error_json_structure_invalid", line, column, field);
		} catch (JsonParseException e) {
			String msg = e.getOriginalMessage();
			String line = "unknown";
			String column = "unknown";
			if (e.getLocation() != null) {
				line = String.valueOf(e.getLocation().getLineNr());
				column = String.valueOf(e.getLocation().getColumnNr());
			}
			throw new GenericRestException(BAD_REQUEST, "error_json_malformed", line, column, msg);
		}
	}

	public static ObjectMapper getMapper() {
		return defaultMapper;
	}

}
