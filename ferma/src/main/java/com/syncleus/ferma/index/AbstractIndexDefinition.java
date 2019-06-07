package com.syncleus.ferma.index;

import com.syncleus.ferma.index.field.FieldMap;

public abstract class AbstractIndexDefinition implements ElementIndexDefinition {

	protected boolean unique = false;

	protected FieldMap fields;

	@Override
	public boolean isUnique() {
		return unique;
	}

	@Override
	public FieldMap getFields() {
		return fields;
	}
}
