package com.gentics.mesh.core.data.model.schema.propertytypes;

import java.util.HashSet;
import java.util.Set;

public class ReferencePropertyTypeSchema extends BasicPropertyTypeSchema {

	private static final long serialVersionUID = -965692160322365637L;

	private Set<String> objectTypeSchemaWhitelist = new HashSet<>();

	public ReferencePropertyTypeSchema(String name) {
		super(name, PropertyType.REFERENCE);
	}

	public Set<String> getObjectTypeSchemaWhitelist() {
		return objectTypeSchemaWhitelist;
	}
}
