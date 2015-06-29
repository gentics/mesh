package com.gentics.mesh.core.rest.schema;

public abstract class AbstractFieldSchema implements FieldSchema {

	private String name;

	private String label;

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getType() {
		//TODO maybe a short name would be better.
		return getClass().getName();
	}

}
