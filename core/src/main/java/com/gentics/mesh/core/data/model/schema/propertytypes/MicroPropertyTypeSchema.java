package com.gentics.mesh.core.data.model.schema.propertytypes;

import java.util.HashSet;
import java.util.Set;


public class MicroPropertyTypeSchema extends AbstractPropertyTypeSchema {

	private static final long serialVersionUID = 1838466275169369495L;

	private Set<BasicPropertyTypeSchema> parts  = new HashSet<>();
	
	public MicroPropertyTypeSchema(String name) {
		super(name, PropertyType.MICROSCHEMA);
	}
	
	public Set<BasicPropertyTypeSchema> getProperties() {
		return parts;
	}
	

}
