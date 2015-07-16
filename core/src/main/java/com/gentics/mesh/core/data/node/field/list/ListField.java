package com.gentics.mesh.core.data.node.field.list;

import java.util.List;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableField;

public interface ListField<T extends ListableField> extends MicroschemaListableField, MeshVertex {

	List<? extends T> getList();

	long getSize();

	void addItem(T item);

	// void getItem(String key);

	void removeItem(T item);

	Class<? extends T> getListType();

}
