package com.gentics.mesh.core.rest.node.field;

public interface DateField extends ListableField, MicroschemaListableField {

	void setDate(String date);

	String getDate();

}
