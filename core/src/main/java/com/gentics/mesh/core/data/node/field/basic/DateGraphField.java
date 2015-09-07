package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.DateField;

public interface DateGraphField extends ListableGraphField, BasicGraphField<DateField> {

	void setDate(String date);

	String  getDate();

}
