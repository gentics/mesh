package com.gentics.mesh.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.BooleanUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HTMLField;
import com.gentics.mesh.core.rest.node.field.ListField;
import com.gentics.mesh.core.rest.node.field.MicroschemaField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.SelectField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HTMLFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.ListFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.MicroschemaFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.SelectFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;

public class FieldMapDeserializer extends JsonDeserializer<Map<String, Field>> {

	@Override
	public Map<String, Field> deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		Schema schema = (Schema) ctxt.findInjectableValue("schema", null, null);
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		Iterator<Entry<String, JsonNode>> it = node.fields();
		Map<String, Field> map = new HashMap<>();
		while (it.hasNext()) {
			Entry<String, JsonNode> currentEntry = it.next();
			String fieldKey = currentEntry.getKey();
			FieldSchema fieldSchema = schema.getFields().get(fieldKey);
			addField(map, fieldKey, fieldSchema, currentEntry.getValue());
		}
		return map;
	}

	private void addField(Map<String, Field> map, String fieldKey, FieldSchema fieldSchema, JsonNode jsonNode) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case HTML:
			HTMLField htmlField = new HTMLFieldImpl();
			htmlField.setHTML(jsonNode.textValue());
			map.put(fieldKey, htmlField);
			break;
		case STRING:
			StringField stringField = new StringFieldImpl();
			stringField.setText(jsonNode.textValue());
			map.put(fieldKey, stringField);
			break;
		case NUMBER:
			NumberField numberField = new NumberFieldImpl();
			numberField.setNumber(jsonNode.textValue());
			map.put(fieldKey, numberField);
			break;
		case BOOLEAN:
			BooleanField booleanField = new BooleanFieldImpl();
			booleanField.setValue(BooleanUtils.toBooleanObject(jsonNode.textValue()));
			map.put(fieldKey, booleanField);
			break;
		case DATE:
			DateField dateField = new DateFieldImpl();
			dateField.setDate(jsonNode.textValue());
			map.put(fieldKey, dateField);
			break;
		case SELECT:
			SelectField selectField = new SelectFieldImpl();
			//TODO impl
			break;
		case LIST:
			ListField listField = new ListFieldImpl();
			//TODO impl
			break;
		case NODE:
			NodeField nodeField = new NodeFieldImpl();
			//TODO impl
			break;
		case MICROSCHEMA:
			MicroschemaField MicroschemaField = new MicroschemaFieldImpl();
			//TODO impl
			break;
		default:
			//TODO handle unknown type situation
			break;
		}
	}
}
