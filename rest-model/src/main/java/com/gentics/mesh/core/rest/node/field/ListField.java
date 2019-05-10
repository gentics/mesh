package com.gentics.mesh.core.rest.node.field;

import java.util.List;

public interface ListField<T extends ListableField> extends Field, MicroschemaListableField {

	List<T> getItems();

	@Override
	default Object getValue() {
		return getItems();
	}
}
