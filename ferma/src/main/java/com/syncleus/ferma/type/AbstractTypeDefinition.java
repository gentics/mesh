package com.syncleus.ferma.type;

import com.syncleus.ferma.index.field.FieldMap;

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
