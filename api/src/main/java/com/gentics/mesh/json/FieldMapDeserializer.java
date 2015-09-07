package com.gentics.mesh.json;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
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
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicroschemaFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

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
			htmlField.setHTML(jsonNode.textValue());
			map.put(fieldKey, htmlField);
			break;
		case STRING:
			StringField stringField = new StringFieldImpl();
			stringField.setString(jsonNode.textValue());
			map.put(fieldKey, stringField);
			break;
		case NUMBER:
			NumberField numberField = new NumberFieldImpl();
			Number number = jsonNode.numberValue();
			if (number == null) {
				throw new MeshJsonException("Could not find number for number field {" + fieldKey + "}");
			}
			numberField.setNumber(number.toString());
			map.put(fieldKey, numberField);
			break;
		case BOOLEAN:
			BooleanField booleanField = new BooleanFieldImpl();
			if (jsonNode.isNull()) {
				booleanField.setValue(null);
			} else {
				booleanField.setValue(jsonNode.booleanValue());
			}
			map.put(fieldKey, booleanField);
			break;
		case DATE:
			DateField dateField = new DateFieldImpl();
			dateField.setDate(jsonNode.textValue());
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
					NodeFieldListImpl nodeListField = null;
					try {
						nodeListField = JsonUtil.readNode(jsonNode.toString(), NodeFieldListImpl.class, schemaStorage);
					} catch (MeshJsonException e) {
						if (log.isDebugEnabled()) {
							log.debug(
									"Could not deserialize json to expanded Node Response this is normal when the json does not contain expanded fields: "
											+ e.getMessage());
						}
						nodeListField = oc.treeToValue(jsonNode, NodeFieldListImpl.class);
					} catch (IOException e) {
						throw new MeshJsonException("Could not read node field for key {" + fieldKey + "}", e);
					}
					map.put(fieldKey, nodeListField);
					break;
				case "number":
					map.put(fieldKey, oc.treeToValue(jsonNode, NumberFieldListImpl.class));
					break;
				case "date":
					map.put(fieldKey, oc.treeToValue(jsonNode, DateFieldListImpl.class));
					break;
				case "boolean":
					map.put(fieldKey, oc.treeToValue(jsonNode, BooleanFieldListImpl.class));
					break;
				case "microschema":
					map.put(fieldKey, oc.treeToValue(jsonNode, MicroschemaFieldListImpl.class));
					break;
				case "string":
					map.put(fieldKey, oc.treeToValue(jsonNode, StringFieldListImpl.class));
					break;
				case "html":
					map.put(fieldKey, oc.treeToValue(jsonNode, HtmlFieldListImpl.class));
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
				//TODO disable bogus output for now
				//				if (log.isDebugEnabled()) {
				//					log.debug("Could not deserialize json to expanded Node Response. I'll try to fallback to a collapsed version of that field.", e);
				//				}
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
}
