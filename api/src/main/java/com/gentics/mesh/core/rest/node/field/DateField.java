package com.gentics.mesh.core.rest.node.field;

public interface DateField extends ListableField, MicroschemaListableField {

	Long getDate();

	DateField setDate(Long date);

}
