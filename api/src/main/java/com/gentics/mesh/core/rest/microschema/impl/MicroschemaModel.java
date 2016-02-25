package com.gentics.mesh.core.rest.microschema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.impl.AbstractFieldSchemaContainer;

/**
 * Implementation of Microschema
 */
public class MicroschemaModel extends AbstractFieldSchemaContainer implements Microschema, RestModel {

	@Override
	public void validate() {
		super.validate();

		Optional<FieldSchema> firstDisallowed = getFields().stream().filter(field -> {
			// Filter for unsupported field types (eg.: micronodes in micronodes and binary fields in micronodes)
			switch (field.getType()) {
			case "binary":
			case "micronode":
				return true;
			case "list":
				ListFieldSchema listField = (ListFieldSchema) field;
				switch (listField.getListType()) {
				case "binary":
				case "micronode":
					return true;
				}
			}
			return false;
		}).findFirst();
		if (firstDisallowed.isPresent()) {
			FieldSchema field = firstDisallowed.get();
			String typeInfo = field.getType();
			if (field instanceof ListFieldSchema) {
				typeInfo = "list:" + ((ListFieldSchema) field).getListType();
			}
			throw error(BAD_REQUEST, "microschema_error_field_type_not_allowed", field.getName(), typeInfo);
		}
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}
}
