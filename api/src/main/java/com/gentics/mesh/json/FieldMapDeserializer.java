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
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.MicroschemaField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.SelectField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.MicroschemaFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.SelectFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicroschemaFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FieldMapDeserializer extends JsonDeserializer<FieldMap> {

	private static final Logger log = LoggerFactory.getLogger(FieldMapDeserializer.class);

	@Override
	public FieldMap deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {

		String schemaName = (String) ctxt.findInjectableValue("schemaName", null, null);
		if (schemaName == null) {
			throw new MeshJsonException("It is not possible to deserialize the field map because the schemaName could not be extracted.");
		}
		SchemaStorage schemaStorage = (SchemaStorage) ctxt.findInjectableValue("schema_storage", null, null);
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		Iterator<Entry<String, JsonNode>> it = node.fields();
		FieldMap map = new FieldMapImpl();
		while (it.hasNext()) {
			Entry<String, JsonNode> currentEntry = it.next();
			String fieldKey = currentEntry.getKey();
			Schema schema = schemaStorage.getSchema(schemaName);
			if (schema == null) {
				throw new MeshJsonException("Can't find schema {" + schemaName + "} within the schema storage.");
			}
			FieldSchema fieldSchema = null;
			for (FieldSchema currentFieldSchema : schemaStorage.getSchema(schemaName).getFields()) {
				if (currentFieldSchema.getName() == null) {
					log.info("Can't handle field schema in schema {" + schemaName + "} because the field schema name was not set.");
				} else if (currentFieldSchema.getName().equals(fieldKey)) {
					fieldSchema = currentFieldSchema;
				}
			}
			if (fieldSchema != null) {
				addField(map, fieldKey, fieldSchema, currentEntry.getValue(), oc, schemaStorage);
			} else {
				throw new MeshJsonException("Can't handle field {" + fieldKey + "} The schema {" + schemaName + "} does not specify this key.");
			}
		}
		return map;
	}

	private void addField(Map<String, Field> map, String fieldKey, FieldSchema fieldSchema, JsonNode jsonNode, ObjectCodec oc,
			SchemaStorage schemaStorage) throws JsonProcessingException {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
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
		case NUMBER:
			NumberField numberField = new NumberFieldImpl();
			if (!jsonNode.isNull() && jsonNode.isNumber()) {
				Number number = jsonNode.numberValue();
				numberField.setNumber(number.toString());
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
			if (!jsonNode.isNull() && jsonNode.isTextual()) {
				dateField.setDate(jsonNode.textValue());
			}
			if (!jsonNode.isNull() && !jsonNode.isTextual()) {
				throw new MeshJsonException("The field value for {" + fieldKey + "} is not a text value. The value was {" + jsonNode.asText() + "}");
			}
			map.put(fieldKey, dateField);
			break;
		case SELECT:
			SelectField selectField = new SelectFieldImpl();
			map.put(fieldKey, selectField);
			// TODO impl
			break;
		case LIST:
			if (fieldSchema instanceof ListFieldSchemaImpl) {
				ListFieldSchemaImpl listFieldSchema = (ListFieldSchemaImpl) fieldSchema;
				switch (listFieldSchema.getListType()) {
				case "node":
					//TODO use  NodeFieldListItemDeserializer to deserialize the item in expanded form
					NodeFieldListItem[] itemsArray = oc.treeToValue(jsonNode, NodeFieldListItemImpl[].class);
					NodeFieldListImpl nodeListField = new NodeFieldListImpl();
					nodeListField.getItems().addAll(Arrays.asList(itemsArray));

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

				case "microschema":
					map.put(fieldKey, oc.treeToValue(jsonNode, MicroschemaFieldListImpl.class));
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
					String[] itemsDateArray = oc.treeToValue(jsonNode, String[].class);
					addBasicList(map, fieldKey, String[].class, new DateFieldListImpl(), String.class, itemsDateArray);
					break;
				case "number":
					String[] itemsNumberArray = oc.treeToValue(jsonNode, String[].class);
					addBasicList(map, fieldKey, String[].class, new NumberFieldListImpl(), String.class, itemsNumberArray);
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
		case MICROSCHEMA:
			MicroschemaField MicroschemaField = new MicroschemaFieldImpl();
			// TODO impl
			break;
		default:
			// TODO handle unknown type situation
			break;
		}
	}

	private <I, AT> void addBasicList(Map<String, Field> map, String fieldKey, Class<AT> clazzOfJsonArray, FieldList<I> list, Class<I> classOfItem,
			I[] itemsArray) throws JsonProcessingException {
		list.getItems().addAll((List<I>) Arrays.asList(itemsArray));
		map.put(fieldKey, list);
	}
}
