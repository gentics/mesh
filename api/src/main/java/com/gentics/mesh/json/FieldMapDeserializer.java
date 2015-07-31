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
				addField(map, fieldKey, fieldSchema, currentEntry.getValue(), oc);
			} else {
				throw new MeshJsonException("Can't handle field {" + fieldKey + "} The schema {" + schemaName + "} does not specify this key.");
			}
		}
		return map;
	}

	private void addField(Map<String, Field> map, String fieldKey, FieldSchema fieldSchema, JsonNode jsonNode, ObjectCodec oc)
			throws JsonProcessingException {
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
			numberField.setNumber(jsonNode.numberValue().toString());
			map.put(fieldKey, numberField);
			break;
		case BOOLEAN:
			BooleanField booleanField = new BooleanFieldImpl();
			//booleanField.setValue(BooleanUtils.toBooleanObject(jsonNode.booleanValue()));
			booleanField.setValue(jsonNode.booleanValue());
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
					map.put(fieldKey, oc.treeToValue(jsonNode, NodeFieldListImpl.class));
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
				}
			} else {
				//TODO handle unexpected error
			}

			//ListField listField = new ListFieldImpl();
			// TODO impl
			//jsonNode.
			//jsonNode.get
			//map.put(fieldKey, listField);
			break;
		case NODE:
//			NodeField nodeField = new NodeFieldImpl();
			map.put(fieldKey, oc.treeToValue(jsonNode, NodeFieldImpl.class));
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
