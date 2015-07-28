package com.gentics.mesh.core.rest.node.field;

public interface NumberField extends ListableField, MicroschemaListableField {

	String getNumber();

	NumberField setNumber(String number);

}
