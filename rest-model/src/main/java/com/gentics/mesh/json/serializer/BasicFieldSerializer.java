package com.gentics.mesh.json.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.BooleanFieldModel;
import com.gentics.mesh.core.rest.node.field.DateFieldModel;
import com.gentics.mesh.core.rest.node.field.FieldModel;
import com.gentics.mesh.core.rest.node.field.HtmlFieldModel;
import com.gentics.mesh.core.rest.node.field.NumberFieldModel;
import com.gentics.mesh.core.rest.node.field.StringFieldModel;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;

/**
 * Serializer which will handle basic fields (eg. String, html, number) which does not have a nested object within the field JSON format.
 *
 * @param <T>
 */
public class BasicFieldSerializer<T extends FieldModel> extends JsonSerializer<T> {

	@Override
	public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {

		if (value instanceof FieldSchema) {
			return;
		} else {

			// Determine the type of the field and the specific deseralisation calls to produce the simplified json
			FieldTypes type = FieldTypes.valueByName(value.getType());
			switch (type) {
			case HTML:
				HtmlFieldModel htmlField = (HtmlFieldImpl) value;
				if (htmlField.getHTML() == null) {
					gen.writeNull();
				} else {
					gen.writeString(htmlField.getHTML());
				}
				break;
			case STRING:
				StringFieldModel stringField = (StringFieldImpl) value;
				if (stringField.getString() == null) {
					gen.writeNull();
				} else {
					gen.writeString(stringField.getString());
				}
				break;
			case NUMBER:
				NumberFieldModel numberField = (NumberFieldImpl) value;
				if (numberField.getNumber() == null) {
					gen.writeNull();
				} else {
					gen.writeNumber(numberField.getNumber().toString());
				}
				break;
			case BOOLEAN:
				BooleanFieldModel booleanField = (BooleanFieldImpl) value;
				if (booleanField.getValue() == null) {
					gen.writeNull();
				} else {
					gen.writeBoolean(booleanField.getValue());
				}
				break;
			case DATE:
				DateFieldModel dateField = (DateFieldImpl) value;
				if (dateField.getDate() == null) {
					gen.writeNull();
				} else {
					gen.writeString(dateField.getDate());
				}
				break;
			default:
				//TODO handle error
				break;
			}
		}
	}
}
