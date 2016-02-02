package com.gentics.mesh.core.rest.microschema.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.impl.AbstractFieldSchemaContainer;
import com.gentics.mesh.json.MeshJsonException;

/**
 * Implementation of Microschema
 */
public class MicroschemaImpl extends AbstractFieldSchemaContainer implements Microschema, RestModel {

	private String description;
	private List<FieldSchema> fields = new ArrayList<>();

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void validate() throws MeshJsonException {
		// TODO check for field types that are not allowed in Microschemas
		List<String> disallowedFieldTypes = Arrays.asList("");
		Optional<FieldSchema> firstDisallowed = fields.stream().filter(field -> disallowedFieldTypes.contains(field.getType())).findFirst();
		if (firstDisallowed.isPresent()) {
			FieldSchema field = firstDisallowed.get();
			throw new MeshJsonException("The field " + field.getName() + " is of type " + field.getType() + " which is not allowed in a microschema");
		}

		if (!fields.stream().map(field -> field.getName()).allMatch(new HashSet<>()::add)) {
			throw new MeshJsonException("The microschema contains duplicate names. The name for a schema field must be unique.");
		}
		if (!fields.stream().map(field -> field.getLabel()).allMatch(new HashSet<>()::add)) {
			throw new MeshJsonException("The microschema contains duplicate labels. The label for a schema field must be unique.");
		}
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}
}
