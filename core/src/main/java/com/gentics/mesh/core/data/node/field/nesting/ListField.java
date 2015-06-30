package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

public interface ListField<T extends ListableField> extends NestingField, MicroschemaListableField {

	List<? extends T> getList();

	void setListType(Class<? extends T> t);

	Class<? extends T> getListType();

}
