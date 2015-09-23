package com.gentics.mesh.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;

public class BasicFieldSerializer<T extends Field> extends JsonSerializer<T> {

	@Override
	public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {

		if (value instanceof FieldSchema) {
			//gen.writeObject(value);
			return;
		} else {
			FieldTypes type = FieldTypes.valueByName(value.getType());
			switch (type) {
			case HTML:
				HtmlField htmlField = (HtmlFieldImpl) value;
				if (htmlField.getHTML() == null) {
					gen.writeNull();
				} else {
					gen.writeString(htmlField.getHTML());
				}
				break;
			case STRING:
				StringField stringField = (StringFieldImpl) value;
				if (stringField.getString() == null) {
					gen.writeNull();
				} else {
					gen.writeString(stringField.getString());
				}
				break;
			case NUMBER:
				NumberField numberField = (NumberFieldImpl) value;
				if (numberField.getNumber() == null) {
					gen.writeNull();
				} else {
					gen.writeNumber(numberField.getNumber());
				}
				break;
			case BOOLEAN:
				BooleanField booleanField = (BooleanFieldImpl) value;
				if (booleanField.getValue() == null) {
					gen.writeNull();
				} else {
					gen.writeBoolean(booleanField.getValue());
				}
				break;
			case DATE:
				DateField dateField = (DateFieldImpl) value;
				if (dateField.getDate() == null) {
					gen.writeNull();
				} else {
					gen.writeNumber(dateField.getDate());
				}
				break;
			//			case NODE:
			//				NodeField nodeField = (NodeFieldImpl) value;
			//				// TODO impl
			//				break;
			//			case LIST:
			//				//TODO just continue with normal deserialization
			//				//ListField listField = (ListFieldImpl) value;
			//				// TODO impl
			//				gen.writeObject(value);
			//				break;
			//			case SELECT:
			//				SelectField selectField = (SelectFieldImpl) value;
			//				// TODO impl
			//				break;
			//			case MICROSCHEMA:
			//				MicroschemaField microschemaField = (MicroschemaFieldImpl) value;
			//				// TODO impl
			//				break;
			default:
				//TODO handle error
				break;
			}
		}
	}
}
