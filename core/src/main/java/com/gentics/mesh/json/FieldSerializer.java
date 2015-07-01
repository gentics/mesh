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

public class FieldSerializer<T extends Field> extends JsonSerializer<T> {

	@Override
	public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
		FieldTypes type = FieldTypes.valueByName(value.getType());
		switch (type) {
		case HTML:
			HTMLField htmlField = (HTMLFieldImpl) value;
			gen.writeString(htmlField.getHTML());
			break;
		case STRING:
			StringField stringField = (StringFieldImpl) value;
			gen.writeString(stringField.getText());
			break;
		case NUMBER:
			NumberField numberField = (NumberFieldImpl) value;
			gen.writeNumber(numberField.getNumber());
			break;
		case BOOLEAN:
			BooleanField booleanField = (BooleanFieldImpl) value;
			gen.writeBoolean(booleanField.getValue());
			break;
		case DATE:
			DateField dateField = (DateFieldImpl) value;
			gen.writeString(dateField.getDate());
			break;
		case NODE:
			NodeField nodeField = (NodeFieldImpl) value;
			//TODO impl
			break;
		case LIST:
			ListField listField = (ListFieldImpl) value;
			//TODO impl
			break;
		case SELECT:
			SelectField selectField = (SelectFieldImpl) value;
			//TODO impl
			break;
		case MICROSCHEMA:
			MicroschemaField microschemaField = (MicroschemaFieldImpl) value;
			//TODO impl
			break;
		}

	}
}
