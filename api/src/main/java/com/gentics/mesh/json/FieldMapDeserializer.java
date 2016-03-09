package com.gentics.mesh.json;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.micronode.NullMicronodeResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The {@link FieldMapDeserializer} is used to deserialize the fieldmap within a node response/update request.
 *
 */
public class FieldMapDeserializer extends JsonDeserializer<FieldMap> {

	private static final Logger log = LoggerFactory.getLogger(FieldMapDeserializer.class);

	@Override
	public FieldMap deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {

		// 1. Load the schema name that was identified by another deserializer and put into the context.
		String schemaName = (String) ctxt.findInjectableValue("schemaName", null, null);
		Integer schemaVersion = (Integer) ctxt.findInjectableValue("schemaVersion", null, null);
		String microschemaName = (String) ctxt.findInjectableValue("microschemaName", null, null);
		Integer microschemaVersion = (Integer) ctxt.findInjectableValue("microschemaVersion", null, null);
		if (schemaName == null && microschemaName == null) {
			throw new MeshJsonException("It is not possible to deserialize the field map because the schemaName could not be extracted.");
		}
		// 2. Load the schema storage that holds the schema for the handled node
		SchemaStorage schemaStorage = (SchemaStorage) ctxt.findInjectableValue("schema_storage", null, null);
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);

		List<? extends FieldSchema> fields = null;
		if (schemaName != null) {
			Schema schema = null;
			if (schemaVersion == null) {
				schema = schemaStorage.getSchema(schemaName);
			} else {
				schema = schemaStorage.getSchema(schemaName, schemaVersion);
			}
			if (schema == null) {
				throw new MeshJsonException("Can't find schema {" + schemaName + "} within the schema storage.");
			}
			fields = schema.getFields();
		} else {
			Microschema microschema = null;
			if (microschemaVersion == null) {
				microschema = schemaStorage.getMicroschema(microschemaName);
			} else {
				microschema = schemaStorage.getMicroschema(microschemaName, microschemaVersion);
			}
			if (microschema == null) {
				throw new MeshJsonException("Can't find microschema {" + microschemaName + "} within the schema storage.");
			}
			fields = microschema.getFields();
		}

		// 3. Iterate over all fields and load the field schema from 
		Iterator<Entry<String, JsonNode>> it = node.fields();
		FieldMap map = new FieldMapImpl();
		while (it.hasNext()) {
			Entry<String, JsonNode> currentEntry = it.next();
			String fieldKey = currentEntry.getKey();

			// Check whether the field with the given key could be found in the schema
			FieldSchema fieldSchema = null;
			for (FieldSchema currentFieldSchema : fields) {
				if (currentFieldSchema.getName() == null) {
					if (schemaName != null) {
						log.info("Can't handle field schema in schema {" + schemaName + "} because the field schema name was not set.");
					} else {
						log.info("Can't handle field schema in microschema {" + microschemaName + "} because the field schema name was not set.");
					}
				} else if (currentFieldSchema.getName().equals(fieldKey)) {
					fieldSchema = currentFieldSchema;
				}
			}
			if (fieldSchema != null) {
				addField(map, fieldKey, fieldSchema, currentEntry.getValue(), jsonParser, schemaStorage);
			} else {
				if (schemaName != null) {
					throw new MeshJsonException("Can't handle field {" + fieldKey + "} The schema {" + schemaName + "} does not specify this key.");
				} else {
					throw new MeshJsonException(
							"Can't handle field {" + fieldKey + "} The microschema {" + microschemaName + "} does not specify this key.");
				}
			}
		}
		return map;
	}

	/**
	 * Deserialize the current field and put it in the provided map.
	 * 
	 * @param map
	 *            Map which holds the deserialize fields
	 * @param fieldKey
	 *            Key of the current field
	 * @param fieldSchema
	 *            Field schema for the current field
	 * @param jsonNode
	 *            JsonNode of the current field
	 * @param jsonParser
	 * @param schemaStorage
	 * @throws IOException
	 */
	private void addField(Map<String, Field> map, String fieldKey, FieldSchema fieldSchema, JsonNode jsonNode, JsonParser jsonParser,
			SchemaStorage schemaStorage) throws IOException {
		ObjectCodec oc = jsonParser.getCodec();
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());

		// Handle each field type
		switch (type) {
		case HTML:
			HtmlField htmlField = new HtmlFieldImpl();
			if (!jsonNode.isNull() && jsonNode.isTextual()) {
				htmlField.setHTML(jsonNode.textValue());
			}
			if (!jsonNode.isNull() && !jsonNode.isTextual()) {
				throw new MeshJsonException("The field value for {" + fieldKey + "} is not a text value. The value was {" + jsonNode.asText() + "}");
			}
			map.put(fieldKey, htmlField);
			break;
		case STRING:
			StringField stringField = new StringFieldImpl();
			if (!jsonNode.isNull() && jsonNode.isTextual()) {
				stringField.setString(jsonNode.textValue());
			}
			if (!jsonNode.isNull() && !jsonNode.isTextual()) {
				throw new MeshJsonException("The field value for {" + fieldKey + "} is not a text value. The value was {" + jsonNode.asText() + "}");
			}
			map.put(fieldKey, stringField);
			break;
		case BINARY:
			BinaryField binaryField = JsonUtil.readValue(jsonNode.toString(), BinaryFieldImpl.class);
			map.put(fieldKey, binaryField);
			break;
		case NUMBER:
			NumberField numberField = new NumberFieldImpl();
			if (!jsonNode.isNull() && jsonNode.isNumber()) {
				Number number = jsonNode.numberValue();
				numberField.setNumber(number);
			}
			if (!jsonNode.isNull() && !jsonNode.isNumber()) {
				throw new MeshJsonException(
						"The field value for {" + fieldKey + "} is not a number value. The value was {" + jsonNode.asText() + "}");
			}
			map.put(fieldKey, numberField);
			break;
		case BOOLEAN:
			BooleanField booleanField = new BooleanFieldImpl();
			if (!jsonNode.isNull() && jsonNode.isBoolean()) {
				booleanField.setValue(jsonNode.booleanValue());
			}
			if (!jsonNode.isNull() && !jsonNode.isBoolean()) {
				throw new MeshJsonException(
						"The field value for {" + fieldKey + "} is not a boolean value. The value was {" + jsonNode.asText() + "}");
			}
			map.put(fieldKey, booleanField);
			break;
		case DATE:
			DateField dateField = new DateFieldImpl();
			if (!jsonNode.isNull() && jsonNode.isNumber()) {
				dateField.setDate(jsonNode.numberValue().longValue());
			}
			if (!jsonNode.isNull() && !jsonNode.isNumber()) {
				throw new MeshJsonException(
						"The field value for {" + fieldKey + "} is not a number value. The value was {" + jsonNode.asText() + "}");
			}
			map.put(fieldKey, dateField);
			break;
		case LIST:
			if (fieldSchema instanceof ListFieldSchemaImpl) {
				ListFieldSchemaImpl listFieldSchema = (ListFieldSchemaImpl) fieldSchema;
				switch (listFieldSchema.getListType()) {
				case "node":
					//TODO use  NodeFieldListItemDeserializer to deserialize the item in expanded form
					NodeFieldListImpl nodeListField = new NodeFieldListImpl();
					NodeFieldListItemDeserializer deser = new NodeFieldListItemDeserializer();
					for (JsonNode node : jsonNode) {
						nodeListField.getItems().add(deser.deserialize(node, jsonParser, schemaStorage));
					}
					//NodeFieldListItem[] itemsArray = oc.treeToValue(jsonNode, NodeFieldListItemImpl[].class);
					//nodeListField.getItems().addAll(Arrays.asList(itemsArray));

					//					NodeFieldListImpl nodeListField = null;
					//					try {
					//						nodeListField = JsonUtil.readNode(jsonNode.toString(), NodeFieldListImpl.class, schemaStorage);
					//					} catch (MeshJsonException e) {
					//						if (log.isDebugEnabled()) {
					//							log.debug(
					//									"Could not deserialize json to expanded Node Response this is normal when the json does not contain expanded fields: "
					//											+ e.getMessage());
					//						}
					//						nodeListField = oc.treeToValue(jsonNode, NodeFieldListImpl.class);
					//					} catch (IOException e) {
					//						throw new MeshJsonException("Could not read node field for key {" + fieldKey + "}", e);
					//					}
					map.put(fieldKey, nodeListField);
					break;

				case "micronode":
					MicronodeFieldList micronodeFieldList = new MicronodeFieldListImpl();
					for (JsonNode node : jsonNode) {
						try {
							micronodeFieldList.getItems().add(JsonUtil.readNode(node.toString(), MicronodeResponse.class, schemaStorage));
						} catch (IOException e) {
							throw new MeshJsonException("", e);
						}
					}

					map.put(fieldKey, micronodeFieldList);
					break;
				// Basic types
				case "string":
					String[] itemsStringArray = oc.treeToValue(jsonNode, String[].class);
					addBasicList(map, fieldKey, String[].class, new StringFieldListImpl(), String.class, itemsStringArray);
					break;
				case "html":
					String[] itemsHtmlArray = oc.treeToValue(jsonNode, String[].class);
					addBasicList(map, fieldKey, String[].class, new HtmlFieldListImpl(), String.class, itemsHtmlArray);
					break;
				case "date":
					Long[] itemsDateArray = oc.treeToValue(jsonNode, Long[].class);
					addBasicList(map, fieldKey, Long[].class, new DateFieldListImpl(), Long.class, itemsDateArray);
					break;
				case "number":
					Number[] itemsNumberArray = oc.treeToValue(jsonNode, Number[].class);
					addBasicList(map, fieldKey, Number[].class, new NumberFieldListImpl(), Number.class, itemsNumberArray);
					break;
				case "boolean":
					Boolean[] itemsBooleanArray = oc.treeToValue(jsonNode, Boolean[].class);
					addBasicList(map, fieldKey, Boolean[].class, new BooleanFieldListImpl(), Boolean.class, itemsBooleanArray);
					break;
				default:
					log.error("Unknown list type {" + listFieldSchema.getListType() + "}");
					break;
				}
			} else {
				//TODO handle unexpected error
			}
			break;
		case NODE:
			// Try to deserialize the field in the expanded version
			try {
				NodeResponse expandedField = JsonUtil.readNode(jsonNode.toString(), NodeResponse.class, schemaStorage);
				map.put(fieldKey, expandedField);
			} catch (MeshJsonException e) {
				if (log.isTraceEnabled()) {
					log.trace("Could not deserialize json to expanded Node Response. I'll try to fallback to a collapsed version of that field.", e);
				}
				NodeFieldImpl collapsedField = oc.treeToValue(jsonNode, NodeFieldImpl.class);
				NodeResponse restNode = new NodeResponse();
				restNode.setUuid(collapsedField.getUuid());
				map.put(fieldKey, restNode);
			} catch (IOException e) {
				throw new MeshJsonException("Could not read node field for key {" + fieldKey + "}", e);
			}
			break;
		case MICRONODE:
			try {
				if (jsonNode.isNull()) {
					map.put(fieldKey, new NullMicronodeResponse());
				} else {
					MicronodeResponse field = JsonUtil.readNode(jsonNode.toString(), MicronodeResponse.class, schemaStorage);
					map.put(fieldKey, field);
				}
			} catch (IOException e) {
				throw new MeshJsonException("Could not read node field for key {" + fieldKey + "}", e);
			}
			break;
		default:
			// TODO handle unknown type situation
			break;
		}
	}

	private <I, AT> void addBasicList(Map<String, Field> map, String fieldKey, Class<AT> clazzOfJsonArray, FieldList<I> list, Class<I> classOfItem,
			I[] itemsArray) throws JsonProcessingException {
		if (itemsArray != null) {
			list.getItems().addAll((List<I>) Arrays.asList(itemsArray));
		}
		map.put(fieldKey, list);
	}
}
