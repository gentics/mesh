package com.gentics.mesh.core.data.model.node.field.basic;

import com.gentics.mesh.core.data.model.node.field.nesting.ListableField;

public interface NumberField extends ListableField, BasicField {

	public void setNumber(String number);

	public String getNumber();
}
