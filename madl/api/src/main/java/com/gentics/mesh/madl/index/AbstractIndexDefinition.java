package com.gentics.mesh.madl.index;

import com.gentics.mesh.madl.field.FieldMap;

public abstract class AbstractIndexDefinition implements ElementIndexDefinition {

	protected String postfix;

	protected String name;

	protected boolean unique = false;

	protected FieldMap fields;

	protected IndexType type;

	@Override
	public boolean isUnique() {
		return unique;
	}

	@Override
	public FieldMap getFields() {
		return fields;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPostfix() {
		return postfix;
	}

	@Override
	public IndexType getType() {
		return type;
	}
}
