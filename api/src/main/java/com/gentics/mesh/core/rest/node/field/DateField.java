package com.gentics.mesh.core.rest.node.field;

public interface DateField extends ListableField, MicroschemaListableField {

	DateField setDate(String date);

	String getDate();

}
