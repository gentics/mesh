package com.gentics.mesh.core.rest.node.response.field;

public abstract class AbstractFieldProperty implements FieldProperty {

	private String name;

	private String label;

	public abstract String getType();

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

}
