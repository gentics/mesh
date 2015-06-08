package com.gentics.mesh.core.data.model.schema.propertytypes;

import com.gentics.mesh.core.data.model.tinkerpop.DynamicProperties;

public interface ReferencePropertyTypeSchema extends BasicPropertyTypeSchema {


	@DynamicProperties
	public Iterable<String> getObjectTypeSchemaWhitelist();

//	public ReferencePropertyTypeSchema(String name) {
//		super(name, PropertyType.REFERENCE);
//	}
//
//	public Set<String> getObjectTypeSchemaWhitelist() {
//		return objectTypeSchemaWhitelist;
//	}
}
