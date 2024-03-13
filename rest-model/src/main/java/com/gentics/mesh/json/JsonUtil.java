package com.gentics.mesh.json;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;

import org.codehaus.jettison.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.gentics.mesh.core.rest.error.AbstractRestException;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.field.ListableField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.ExpandableNode;
import com.gentics.mesh.json.deserializer.EventCauseInfoDeserializer;
import com.gentics.mesh.json.deserializer.FieldDeserializer;
import com.gentics.mesh.json.deserializer.FieldMapDeserializer;
import com.gentics.mesh.json.deserializer.FieldSchemaDeserializer;
import com.gentics.mesh.json.deserializer.JsonArrayDeserializer;
import com.gentics.mesh.json.deserializer.JsonObjectDeserializer;
import com.gentics.mesh.json.deserializer.NodeFieldListItemDeserializer;
import com.gentics.mesh.json.deserializer.PermissionChangedEventModelDeserializer;
import com.gentics.mesh.json.deserializer.RestExceptionDeserializer;
import com.gentics.mesh.json.deserializer.UserNodeReferenceDeserializer;
import com.gentics.mesh.json.serializer.BasicFieldSerializer;
import com.gentics.mesh.json.serializer.FieldListSerializer;
import com.gentics.mesh.json.serializer.JsonArraySerializer;
import com.gentics.mesh.json.serializer.JsonObjectSerializer;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Main JSON Util which is used to register all custom JSON specific handlers and deserializers.
 */
public final class JsonUtil {

	protected static ObjectMapper defaultMapper;
	protected static JsonSchemaGenerator schemaGen;
	protected static PrettyPrinter minifyingPrettyPrinter;

	private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

	static {
		initDefaultMapper();
		initSchemaMapper();
	}

	/**
	 * Initialize the default mapper.
	 */
	private static void initDefaultMapper() {
		minifyingPrettyPrinter = new MinimalPrettyPrinter();

		defaultMapper = new ObjectMapper();
		defaultMapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(Include.NON_NULL, Include.ALWAYS));
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
		module.addSerializer(JsonObject.class, new JsonObjectSerializer());
		module.addSerializer(JsonArray.class, new JsonArraySerializer());

		module.addSerializer(FieldMapImpl.class, new JsonSerializer<FieldMapImpl>() {
			@Override
			public void serialize(FieldMapImpl value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
				gen.writeObject(value.getNode());
			}
		});

		module.addDeserializer(JsonObject.class, new JsonObjectDeserializer());
		module.addDeserializer(JsonArray.class, new JsonArrayDeserializer());
		module.addDeserializer(FieldMap.class, new FieldMapDeserializer());
		module.addDeserializer(ExpandableNode.class, new UserNodeReferenceDeserializer());
		module.addDeserializer(ListableField.class, new FieldDeserializer<ListableField>());
		module.addDeserializer(FieldSchema.class, new FieldSchemaDeserializer<FieldSchema>());
		module.addDeserializer(EventCauseInfo.class, new EventCauseInfoDeserializer());
		module.addDeserializer(PermissionChangedEventModel.class, new PermissionChangedEventModelDeserializer());

		defaultMapper.registerModule(module);
		defaultMapper.registerModule(new SimpleModule("interfaceMapping") {
			private static final long serialVersionUID = -4667167382238425197L;

			@Override
			public void setupModule(SetupContext context) {
				addAbstractMapping(context, SchemaModel.class, SchemaModelImpl.class);
				addAbstractMapping(context, MicroschemaModel.class, MicroschemaModelImpl.class);
				addAbstractMapping(context, SchemaReference.class, SchemaReferenceImpl.class);
				addAbstractMapping(context, MicroschemaReference.class, MicroschemaReferenceImpl.class);
			}
		});

	}

	/**
	 * Adds a simple mapping from an abstract type (interface or abstract class) to a single concrete type.
	 *
	 * @param context
	 * @param abstractType
	 * @param concreteType
	 * @param <T>
	 */
	private static <T> void addAbstractMapping(Module.SetupContext context, Class<T> abstractType, Class<? extends T> concreteType) {
		context.addAbstractTypeResolver(new SimpleAbstractTypeResolver().addMapping(abstractType, concreteType));
	}

	/**
	 * Setup the JSON schema generator.
	 */
	private static void initSchemaMapper() {
		ObjectMapper mapper = new ObjectMapper();
		// configure mapper, if necessary, then create schema generator
		schemaGen = new JsonSchemaGenerator(mapper);
	}


	/**
	 * Transform the given object into a JSON string.
	 * 
	 * @param obj
	 * @return
	 * @throws GenericRestException
	 */
	public static <T> String toJson(T obj) throws GenericRestException {
		return toJson(obj, true);
	}

	/**
	 * Transform the given object into a JSON string, considering the HTTP server config.
	 * 
	 * @param obj
	 * @param config
	 * @return
	 * @throws GenericRestException
	 */
	public static <T> String toJson(T obj, boolean minify) throws GenericRestException {
		if (obj instanceof JSONObject) {
			return ((JSONObject) obj).toString();
		}
		try {
			if (minify) {
				return defaultMapper.writer(minifyingPrettyPrinter).writeValueAsString(obj);
			} else {
				return defaultMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
			}
		} catch (IOException e) {
			// TODO i18n
			String message = "Could not generate json from object";
			// TODO 500?
			throw new GenericRestException(INTERNAL_SERVER_ERROR, message, e);
		}
	}

	/**
	 * Transform the given JSON content back into a POJO.
	 * 
	 * @param content
	 *            JSON string
	 * @param valueType
	 *            Class of the POJO
	 * @return POJO instance
	 * @throws GenericRestException
	 *             Exception which contains information about the JSON error line, column
	 */
	public static <T> T readValue(String content, Class<T> valueType) throws GenericRestException {
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
			throw new GenericRestException(BAD_REQUEST, "error_json_structure_invalid", line, column, field, e.getOriginalMessage());
		} catch (JsonParseException e) {
			String msg = e.getOriginalMessage();
			String line = "unknown";
			String column = "unknown";
			if (e.getLocation() != null) {
				line = String.valueOf(e.getLocation().getLineNr());
				column = String.valueOf(e.getLocation().getColumnNr());
			}
			throw new GenericRestException(BAD_REQUEST, "error_json_malformed", line, column, msg);
		} catch (Exception e) {
			throw new GenericRestException(BAD_REQUEST, "error_json_parse", e);
		}
	}

	/**
	 * Generate the JSON schema for the given model class.
	 * 
	 * @param clazz
	 *            Model class
	 * @return
	 */
	public static String getJsonSchema(Class<?> clazz) {
		try {
			com.fasterxml.jackson.module.jsonSchema.JsonSchema schema = schemaGen.generateSchema(clazz);
			return defaultMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
		} catch (Exception e) {
			throw new GenericRestException(INTERNAL_SERVER_ERROR, "error_internal", e);
		}
	}

	/**
	 * Return the JSON object mapper.
	 * 
	 * @return
	 */
	public static ObjectMapper getMapper() {
		return defaultMapper;
	}
}
