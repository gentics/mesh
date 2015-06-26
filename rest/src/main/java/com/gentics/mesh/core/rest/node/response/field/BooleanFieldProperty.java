package com.gentics.mesh.core.rest.node.response.field;

public class BooleanFieldProperty extends AbstractFieldProperty {

	private Boolean value;

	public Boolean getValue() {
		return value;
	}

	public void setValue(Boolean value) {
		this.value = value;
	}

	@Override
	public String getType() {
		return PropertyFieldTypes.BOOLEAN.toString();
	}
}
