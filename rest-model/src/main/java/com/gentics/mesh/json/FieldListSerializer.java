package com.gentics.mesh.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.gentics.mesh.core.rest.node.field.list.FieldList;

/**
 * Serializer for simplified field lists. The serializer will create a json array using the given field list items.
 */
public class FieldListSerializer extends JsonSerializer<FieldList> {

	@Override
	public void serialize(FieldList value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
		if (value == null || value.getItems() == null) {
			gen.writeNull();
		} else {
			gen.writeStartArray();
			for (Object item : value.getItems()) {
				gen.writeObject(item);
			}
			gen.writeEndArray();
		}
	}

}
