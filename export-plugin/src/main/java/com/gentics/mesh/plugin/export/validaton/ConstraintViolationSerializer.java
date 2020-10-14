package com.gentics.mesh.plugin.export.validaton;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import javax.validation.ConstraintViolation;
import java.io.IOException;

public class ConstraintViolationSerializer extends StdSerializer<ConstraintViolation> {
    public ConstraintViolationSerializer() {
        super(ConstraintViolation.class);
    }

    @Override
    public void serialize(ConstraintViolation constraintViolation, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("propertyPath", constraintViolation.getPropertyPath().toString());
        jsonGenerator.writeStringField("message", constraintViolation.getMessage());
        jsonGenerator.writeEndObject();
    }
}
