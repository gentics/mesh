package com.gentics.mesh.core.rest.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Optional;

/**
 * Interface for a Microschema. A microschema can be used to create objects that are embedded into other objects.
 */
public interface MicroschemaModel extends FieldSchemaContainer {

	@Override
	default void validate() {
		FieldSchemaContainer.super.validate();

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

}
