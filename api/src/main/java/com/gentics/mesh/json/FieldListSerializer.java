package com.gentics.mesh.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.gentics.mesh.core.rest.node.field.ListableField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.list.impl.AbstractFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicroschemaFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;

public class FieldListSerializer<T extends AbstractFieldList<?>> extends JsonSerializer<T> {

	@Override
	public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
		gen.writeStartObject();
		gen.writeFieldName("totalCount");
		//TODO fetch value
		gen.writeNumber(-1);

		gen.writeFieldName("order");
		//TODO fetch value
		gen.writeString("desc");

		gen.writeFieldName("orderBy");
		//TODO fetch value
		gen.writeString("name");

		if (value instanceof NodeFieldListImpl) {
			gen.writeObjectField("type", "node");
			gen.writeFieldName("items");
			gen.writeStartArray();
			for (ListableField field : value.getList()) {
				NodeField nodeField = (NodeField) field;
				gen.writeStartObject();
				gen.writeFieldName("uuid");
				gen.writeString(nodeField.getUuid());
				gen.writeEndObject();
			}
			gen.writeEndArray();
		} else if (value instanceof StringFieldListImpl) {
			gen.writeObjectField("type", "string");
		} else if (value instanceof NumberFieldListImpl) {
			gen.writeObjectField("type", "number");
		} else if (value instanceof BooleanFieldListImpl) {
			gen.writeObjectField("type", "boolean");
		} else if (value instanceof HtmlFieldListImpl) {
			gen.writeObjectField("type", "html");
		} else if (value instanceof MicroschemaFieldListImpl) {
			gen.writeObjectField("type", "microschema");
		} else if (value instanceof DateFieldListImpl) {
			gen.writeObjectField("type", "date");
		} else {
			gen.writeNull();
		}
		gen.writeEndObject();
	}

}
