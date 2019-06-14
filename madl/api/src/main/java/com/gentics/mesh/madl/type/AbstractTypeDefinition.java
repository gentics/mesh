package com.gentics.mesh.madl.type;

import com.gentics.mesh.madl.field.FieldMap;

public abstract class AbstractTypeDefinition implements ElementTypeDefinition {

	protected Class<?> superClazz;
	protected FieldMap fields;

	@Override
	public Class<?> getSuperClazz() {
		return superClazz;
	}

	@Override
	public FieldMap getFields() {
		return fields;
	}

}
