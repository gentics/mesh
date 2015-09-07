package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.NumberField;

public interface NumberGraphField extends ListableGraphField, BasicGraphField<NumberField> {

	public void setNumber(String number);

	public String getNumber();
}
